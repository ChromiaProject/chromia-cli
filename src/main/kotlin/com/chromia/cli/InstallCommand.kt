package com.chromia.cli

import com.chromia.cli.lib.GitRepositoryCloner
import com.chromia.cli.lib.InstallDirTarget
import com.chromia.cli.lib.RepositoryCloner
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.util.libraryOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.validate
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class InstallCommand(
        private val repositoryClonerFactory: () -> RepositoryCloner = { GitRepositoryCloner() },
) : CliktCommand(help = "Install library dependencies, if no library specified all will be installed") {
    private val settings by settingsOption()
    private val library by libraryOption().multiple()
            .validate { require(settings.libs.keys.containsAll(it)) { "Specified library(s) $it does not exist in config file" } }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {

        if (library.isEmpty()) {
            settings.libs.forEach { (name, rellLibrary) ->
                fetchLib(name, rellLibrary)
            }
        } else {
            library.forEach { fetchLib(it, settings.libs[it]!!) }
        }
    }

    private fun fetchLib(name: String, rellLibrary: RellLibraryModel) {
        val libraryTarget = Path(settings.source.absolutePath, InstallDirTarget.SOURCE.target).resolve(name)
        val tempDir = Path(settings.target.absolutePath, InstallDirTarget.TEMP.target).resolve(name)
        try {
            repositoryClonerFactory().clone(rellLibrary.registry, tempDir.toFile(), rellLibrary.tagOrBranch)
            val files = readFiles(tempDir.resolve(rellLibrary.path))
            if (!libraryTarget.toFile().exists()) {
                libraryTarget.toFile().mkdirs()
            } else {
                echo("Reinstalling ${libraryTarget.fileName}")
                cleanUpFiles(libraryTarget)
            }

            rellLibrary.verify(files, name)
            copyDir(files, tempDir.resolve(rellLibrary.path), libraryTarget)


        } catch (e: InvalidRemoteException) {
            echo("Invalid remote host. Error: ${e.message}")
        } finally {
            cleanUpFiles(tempDir)
        }
    }

    private fun cleanUpFiles(target: Path) {
        target.toFile().deleteRecursively()
    }

    private fun readFiles(src: Path): List<File> {
        val returnList: MutableList<File> = mutableListOf()
        Files.walk(src).forEach {
            returnList.add(it.toFile())
        }
        return returnList
    }

    private fun copyDir(files: List<File>, src: Path, dest: Path) {
        files.forEach {
            Files.copy(it.toPath(), dest.resolve(src.relativize(it.toPath())),
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
