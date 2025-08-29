package com.chromia.cli.command.library.management

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.DependencyUpdatedMarker
import com.chromia.library.chain.versioning.external.getLatestLibraryVersion
import com.chromia.library.chain.versioning.external.getLibrary
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.chromia.library.chain.versioning.external.getLibraryVersionFilesInBytes
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.arguments.validate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteRecursively
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.moveTo
import kotlin.io.path.writeBytes

class InstallLibraryCommand(
    val repositoryClonerFactory: (quiet: Boolean) -> RepositoryCloner = { GitRepositoryCloner(quiet = it) }
) : AbstractLibraryCommand(
    name = "install",
    help = "Install library dependencies"
) {

    private val explicitLibraryId by argument()
        .optional()
        .validate { it.matches(Regex("^[a-zA-Z0-9._]+$")) }
    private val explicitRegistry by argument()
        .optional()
        .validate {
            require(it.isValidIdentifierOrUrl()) {
                "Library ID must be a valid identifier (alphanumeric, dots, underscores) or URL"
            }
        }

    private val force by option(
        "-f",
        "--force",
        help = "Force installation even if RID verification fails. This bypasses integrity checks " +
            "and should only be used if you trust the source. Use with caution as it may install " +
            "corrupted or tampered libraries."
    ).flag(default = false)

    override fun run() = runCatching {
        val sourceDir = settings.model
            ?.compile
            ?.source
            ?: throw PrintMessage("No compile source directory found in config file")

        val libRoot = sourceDir
            .resolve("lib")
            .also { Files.createDirectories(it) }

        explicitLibraryId?.let {
            downloadAndInstallLibrary(it, RellLibraryModel(explicitRegistry), libRoot, true)
            return@runCatching
        }

        val (chromiaLibs, gitRegistryLibs) = settings.model?.libs
            ?.entries
            ?.partition { it.value.version != null }
            ?: return

        if (gitRegistryLibs.isNotEmpty()) {
            installExternalGitLibraries(gitRegistryLibs.map { it.key })
        }

        runBlocking {
            coroutineScope {
                chromiaLibs.forEach { (libraryId, libModel) ->
                    launch {
                        downloadAndInstallLibrary(libraryId, libModel, libRoot)
                    }
                }
            }
        }

        if (chromiaLibs.isNotEmpty() || gitRegistryLibs.isNotEmpty()) {
            echo("Dependencies installed successfully to ${settings.model?.compile?.source}")
            settings.model?.compile?.target?.let {
                DependencyUpdatedMarker(it.toFile()).markAsInstalled()
            }
        }
    }.getOrThrow()

    @OptIn(ExperimentalPathApi::class)
    private fun downloadAndInstallLibrary(
        libraryId: String,
        libModel: RellLibraryModel,
        libRoot: Path,
        isExplicitInstall: Boolean = false
    ) {
        val (registry, version) = resolveVersionAndRegistry(libraryId, libModel, isExplicitInstall)
        val name = createConfiguredClient(registry)
            .getLibrary(libraryId)
            ?.displayName
            ?: throw PrintMessage("Unable to get library name for $libraryId")

        val expectedRid = createConfiguredClient(libModel.registry)
            .getLibraryRid(libraryId, version!!)
            ?: throw PrintMessage("Unable to get rid for $libraryId")

        val tempLibraryDir = createTempDirectory(name)
        val targetDir = libRoot / name

        try {
            fetchAllLibraryChunks(libraryId, libModel, version)
                .flatMap { it.files.entries }
                .filter { (filePath, _) -> shouldInstallFile(filePath, libModel) }
                .forEach { (relativePath, content) ->
                    val tempPath = tempLibraryDir / relativePath
                    installFile(tempPath, content.data)
                }

            val calculatedRid = calculateRid(tempLibraryDir)

            if (calculatedRid.contentEquals(expectedRid) || force) {
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                targetDir.parent?.createDirectories()
                tempLibraryDir.moveTo(targetDir, StandardCopyOption.REPLACE_EXISTING)
                if (isExplicitInstall) {
                    // TODO: Evaluate if we are able to write directly to yaml file
                    echo(
                        """
                        add this into chromia.yaml file under libs :
                        $libraryId:
                            version: $version
                            registry: $registry
                        """.trimIndent()
                    )
//                    addNewLibraryToChromiaModel(libraryId, libModel, version)
                }
            } else {
                throw PrintMessage(
                    "The hash of the library has changed. " +
                        "This could indicate that files has been corrupted or tampered with." +
                        " Use --force to install anyway."
                )
            }
        } finally {
            if (tempLibraryDir.exists()) {
                tempLibraryDir.deleteRecursively()
            }
        }
    }

    private fun resolveVersionAndRegistry(libraryId: String, libModel: RellLibraryModel, isExplicitInstall: Boolean) =
        if (isExplicitInstall) {
            val latestVersion = createConfiguredClient(explicitRegistry)
                .getLatestLibraryVersion(libraryId)
                ?.version
                ?: throw PrintMessage("Unable to get latest version for $libraryId")
            Pair(explicitRegistry, latestVersion)
        } else {
            Pair(libModel.registry, libModel.version)
        }

//    private fun addNewLibraryToChromiaModel(libraryId: String, libModel: RellLibraryModel, version: String) {
//        val projectDir = settings.model?.compile?.source?.parent
//            ?: throw PrintMessage("Unable to determine project directory")
//
//        // FIXME: can be anything e.g: chromia.yaml OR chromia_devnet.yaml
//        // TODO: check chromia-cli-tools for a function that looks for correct chromia yaml file
//        val chromiaYmlFile = projectDir.resolve("chromia.yml").toFile()
//        if (!chromiaYmlFile.exists()) {
//            throw PrintMessage("chromia.yml file not found in project directory: $projectDir")
//        }
//
//        try {
//            val currentModel = settings.model!!
//            val newLibraryModel = libModel.copy(
//                version = version
//            )
//
//            val modelWithNewLib = currentModel.copy(
//                libs = currentModel.libs + (libraryId to newLibraryModel)
//            )
//
//            modelWithNewLib.writeYaml(chromiaYmlFile.toPath())
//
//            echo("Updated chromia.yml with library: $libraryId (version: $version)")
//        } catch (e: Exception) {
//            echo("Warning: Failed to update chromia.yml: ${e.message}", err = true)
//        }
//    }

    private fun fetchAllLibraryChunks(libraryId: String, libModel: RellLibraryModel, version: String) = run {
        generateSequence(0L) { it + 1 }
            .map { offset -> fetchLibraryChunk(libraryId, libModel, offset, version) }
            .takeWhile { it != null }
            .filterNotNull()
    }

    private fun shouldInstallFile(filePath: String, libModel: RellLibraryModel): Boolean =
        filePath.endsWith(".rell") &&
            (libModel.path?.let { filePath.startsWith(it) } ?: true)

    private fun installFile(targetPath: Path, content: ByteArray) {
        targetPath.parent?.let { Files.createDirectories(it) }
        targetPath.writeBytes(content)
    }

    private fun installExternalGitLibraries(otherLibs: List<String>) {
        ChromiaLibrariesApi.install(
            CliktCliEnv(this),
            settings.model!!.filterLibraries(otherLibs),
            repositoryClonerFactory(!terminal.terminalInfo.outputInteractive),
        )
    }

    private fun fetchLibraryChunk(libraryId: String, libModel: RellLibraryModel, offset: Long, version: String) =
        createConfiguredClient(libModel.registry)
            .getLibraryVersionFilesInBytes(libraryId, version, TEN_FILES, offset)
            .takeIf { it.files.isNotEmpty() }

    private fun String.isValidIdentifierOrUrl(): Boolean {
        val identifierPattern = Regex("^[a-zA-Z0-9._]+$")
        val urlPattern = Regex("^(https?|ftp)://[^\\s/$.?#].\\S*$", RegexOption.IGNORE_CASE)

        return matches(identifierPattern) || matches(urlPattern)
    }

    companion object {
        const val TEN_FILES = 10L
    }
}
