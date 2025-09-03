package com.chromia.cli.command.library.management

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.DependencyUpdatedMarker
import com.chromia.cli.util.libraryOption
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
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.collections.emptyList
import kotlin.io.path.*

class InstallLibraryCommand(
    val repositoryClonerFactory: (quiet: Boolean) -> RepositoryCloner = { GitRepositoryCloner(quiet = it) }
) : AbstractLibraryCommand(
    name = "install",
    help = "Install library dependencies",
    hideKeyPairSourceHelpMessage = true
) {

    private val library by libraryOption()
        .multiple(emptyList(), required = false)
        .validate {
            require(settings.model?.libs?.keys?.containsAll(it) == true) {
                "Specified library(s) $it does not exist in config file"
            }
        }
    private val explicitLibraryId by argument(
        name = "library-id",
        help = "ID of the library to install with optional version, e.g. 'chromia-lib@1.0.0'. " +
            "if no version is specified the latest version will be installed"
    ).optional()
    private val explicitRegistry by argument(
        name = "registry",
        help = "Registry where the library is hosted"
    ).optional()
        .validate {
            require(it.isValidIdentifierOrUrl()) {
                "Registry must be a valid identifier or URL"
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

        val filteredModel = settings.model?.filterLibraries(library.takeIf { it.isNotEmpty() })

        explicitLibraryId?.let {
            val (libraryId, version) = extractLibraryIdAndVersion(it)
            downloadAndInstallLibrary(libraryId, RellLibraryModel(explicitRegistry, version = version), libRoot, true)
            return@runCatching
        }

        val (chromiaLibs, gitRegistryLibs) = filteredModel?.libs
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

    private fun extractLibraryIdAndVersion(libraryIdWithVersion: String): Pair<String, String?> {
        val delimiter = "@"
        return if (libraryIdWithVersion.contains(delimiter)) {
            libraryIdWithVersion.substringBeforeLast(delimiter) to libraryIdWithVersion.substringAfterLast(delimiter)
        } else {
            libraryIdWithVersion to null
        }
    }

    @OptIn(ExperimentalPathApi::class)
    private fun downloadAndInstallLibrary(
        libraryId: String,
        libModel: RellLibraryModel,
        libRoot: Path,
        isExplicitInstall: Boolean = false
    ) {
        val client = createConfiguredClient(libModel.registry)
        val version = resolveVersion(libraryId, libModel, isExplicitInstall)
        val name = client.getLibrary(libraryId)?.displayName
            ?: throw PrintMessage("Library '$libraryId' not found.")

        val expectedRid = client.getLibraryRid(libraryId, version)
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
                tempLibraryDir.copyToRecursively(targetDir, overwrite = true, followLinks = false)
                if (isExplicitInstall) {
                    // TODO: Evaluate if we are able to write directly to yaml file
                    echo(
                        buildString {
                            appendLine("add this into chromia.yaml file under libs :")
                            appendLine(libraryId)
                            appendLine("\tversion: $version")
                            explicitRegistry?.let { appendLine("\tregistry: $it") }
                            remoteTarget.brid?.let { appendLine("\tbrid: $it") }
                        }
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

    private fun resolveVersion(libraryId: String, libModel: RellLibraryModel, isExplicitInstall: Boolean): String {
        return if (isExplicitInstall && libModel.version.isNullOrBlank()) {
            createConfiguredClient(libModel.registry)
                .getLatestLibraryVersion(libraryId)
                ?.version
                ?: throw PrintMessage("Library '$libraryId' is not found.")
        } else {
            libModel.version
                ?: throw PrintMessage("$libraryId version cannot be null.")
        }
    }

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
        val urlPattern = """^(https?://)?([a-zA-Z0-9.-]+\.[a-zA-Z]{2,})(:\d+)?(/.*)?$""".toRegex()

        return matches(identifierPattern) || matches(urlPattern)
    }

    companion object {
        const val TEN_FILES = 10L
    }
}
