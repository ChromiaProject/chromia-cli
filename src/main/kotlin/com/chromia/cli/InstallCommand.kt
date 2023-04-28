package com.chromia.cli

import com.chromia.cli.util.GitRepositoryCloner
import com.chromia.cli.util.RepositoryCloner
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory

class InstallCommand(
        private val repositoryClonerFactory: () -> RepositoryCloner = { GitRepositoryCloner() },
) : CliktCommand(help = "Install libs dependencies") {
    private val settings by settingsOption()
    private val target by option(help = "Explicitly set target directory")
            .defaultLazy("libs") { settings.target.absolutePath }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        settings.libs.forEach { (name, rellLibrary) ->
            try {
                val libraryTarget = Path(target, "libs").resolve(name)
                val tempDir = Path(target, System.currentTimeMillis().toString())

                repositoryClonerFactory().clone(rellLibrary.registry, tempDir.toFile())
                val files = filterFiles(tempDir.resolve(rellLibrary.lib))
                if (!libraryTarget.toFile().exists()) {
                    libraryTarget.toFile().mkdirs()
                } else {
                    echo("Reinstalling ${libraryTarget.fileName}")
                    libraryTarget.toFile().deleteRecursively()
                }

                if (!rellLibrary.validateRid(files)) {
                    throw PrintMessage("The rid ${rellLibrary.rid} for library $name does not match the calculated rid from the downloaded library, can not verify it has not be tampered with")
                }

                copyDir(files, tempDir.resolve(rellLibrary.lib), libraryTarget)
                tempDir.toFile().deleteRecursively()

            } catch (e: InvalidRemoteException) {
                echo("Invalid remote host. Error: ${e.message}")
            }
        }
    }

    private fun filterFiles(src: Path): List<File> {
        val returnList: MutableList<File> = mutableListOf()
        Files.walk(src).forEach {
            if (!it.isDirectory() && it.extension != "rell") {
                return@forEach
            }
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
