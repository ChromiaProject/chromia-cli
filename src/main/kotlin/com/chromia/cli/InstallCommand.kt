package com.chromia.cli

import com.chromia.cli.exception.LibraryMisMatchException
import com.chromia.cli.util.GitRepositoryCloner
import com.chromia.cli.util.RepositoryCloner
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path

class InstallCommand(
        private val repositoryClonerFactory: () -> RepositoryCloner = { GitRepositoryCloner() },
) : CliktCommand(help = "Install libs dependencies") {
    private val settings by settingsOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        //TODO create a file that is called lib installer for this logic
        settings.libs.forEach { (name, rellLibrary) ->
            try {
                val libraryTarget = Path(settings.source.absolutePath, "lib").resolve(name)
                val tempDir = Path(settings.target.absolutePath, ".lib").resolve(name)

                //TODO make it so file name is persistent between clones
                repositoryClonerFactory().clone(rellLibrary.registry, tempDir.toFile())
                val files = readFiles(tempDir.resolve(rellLibrary.path))
                if (!libraryTarget.toFile().exists()) {
                    libraryTarget.toFile().mkdirs()
                } else {
                    echo("Reinstalling ${libraryTarget.fileName}")
                    libraryTarget.toFile().deleteRecursively()
                }

                val (isValid, rid) = rellLibrary.isValid(files)

                if (!isValid) {
                    throw LibraryMisMatchException(rellLibrary.rid?.toHex() ?: "", rid?.toHex() ?: "", name)
                }

                copyDir(files, tempDir.resolve(rellLibrary.path), libraryTarget)
                tempDir.toFile().deleteRecursively()

            } catch (e: InvalidRemoteException) {
                echo("Invalid remote host. Error: ${e.message}")
            }
        }
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
