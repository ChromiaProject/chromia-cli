package com.chromia.cli.command.library.management

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.tools.formatter.warning
import com.chromia.library.chain.versioning.external.getLibraryRid
import com.chromia.library.chain.versioning.external.getLibraryVersionFilesInBytes
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
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

        val (chromiaLibs, otherLibs) = settings.model?.libs
            ?.entries
            ?.partition { it.value.version != null }
            ?: return

        if (otherLibs.isNotEmpty()) {
            installExternalGitLibraries(otherLibs.map { it.key })
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
        
        if (chromiaLibs.isNotEmpty() || otherLibs.isNotEmpty()) {
            echo("Dependencies installed successfully to ${settings.model?.compile?.source}")
        }
    }.getOrElse { e ->
        echo("Failed to install dependencies: ${e.message}", err = true)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun downloadAndInstallLibrary(libraryId: String, libModel: RellLibraryModel, libRoot: Path) {
        val expectedRid = createConfiguredClient(libModel.registry, libModel.brid)
            .getLibraryRid(libraryId, libModel.version!!)
            ?: throw PrintMessage("Unable to get rid for $libraryId")

        val tempDir = createTempDirectory("library-install-$libraryId")
        val targetDir = libRoot / libraryId

        try {
            fetchAllLibraryChunks(libraryId, libModel)
                .flatMap { it.files.entries }
                .filter { (filePath, _) -> shouldInstallFile(filePath, libModel) }
                .forEach { (relativePath, content) ->
                    val tempPath = tempDir / libraryId / relativePath
                    installFile(tempPath, content.data)
                }

            val tempLibraryDir = tempDir / libraryId
            val calculatedRid = calculateRid(tempLibraryDir)

            if (calculatedRid.contentEquals(expectedRid) || force) {
                if (targetDir.exists()) {
                    targetDir.deleteRecursively()
                }

                targetDir.parent?.createDirectories()
                tempLibraryDir.moveTo(targetDir, StandardCopyOption.REPLACE_EXISTING)

                if (!calculatedRid.contentEquals(expectedRid) && force) {
                    echo(
                        warning(
                            "The hash of the library has changed. proceeding anyway due to --force flag"
                        )
                    )
                }
            } else {
                throw PrintMessage(
                    "The hash of the library has changed. " +
                        "This could indicate that files has been corrupted or tampered with." +
                        " Use --force to install anyway."
                )
            }
        } finally {
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun fetchAllLibraryChunks(libraryId: String, libModel: RellLibraryModel) =
        generateSequence(0L) { it + 1 }
            .map { offset -> fetchLibraryChunk(libraryId, libModel, offset) }
            .takeWhile { it != null }
            .filterNotNull()

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

    private fun fetchLibraryChunk(libraryId: String, libModel: RellLibraryModel, offset: Long) =
        createConfiguredClient(libModel.registry, libModel.brid)
            .getLibraryVersionFilesInBytes(libraryId, libModel.version!!, TEN_FILES, offset)
            .takeIf { it.files.isNotEmpty() }

    companion object {
        const val TEN_FILES = 10L
    }
}
