package com.chromia.cli

import com.chromia.cli.util.BaseDependencyResolver
import com.chromia.cli.util.DependencyResolver
import com.chromia.cli.util.GitRepositoryCloner
import com.chromia.cli.util.RepositoryCloner
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import org.eclipse.jgit.api.errors.InvalidRemoteException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.createTempDirectory

class InstallCommand(
        private val repositoryClonerFactory: () -> RepositoryCloner = { GitRepositoryCloner() },
        private val dependencyResolver: () -> DependencyResolver = { BaseDependencyResolver() }
) : CliktCommand(help = "Install libs dependencies") {
    private val settings by settingsOption()
    private val target by option(help = "Explicitly set target directory")
            .defaultLazy("libs") { settings.target.absolutePath + "/libs" }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        dependencyResolver().getDependencies(settings).forEach { (name, rellLibrary) ->
            try {
                val dir = createTempDirectory()
                val registeredLib = dependencyResolver().getLib(name, rellLibrary)

                repositoryClonerFactory().clone(rellLibrary.registry, dir.toFile())
                val targetPath = Path(target, registeredLib.name)
                if (!Path(target, registeredLib.name).toFile().exists()) {
                    targetPath.toFile().mkdirs()
                }

                val resolvePath = dir.resolve(rellLibrary.lib)
                copyDir(resolvePath, targetPath)
                dir.toFile().deleteRecursively()

            } catch (e: InvalidRemoteException) {
                echo("Invalid remote host. Error: ${e.message}")
            }
        }
    }

    private fun copyDir(src: Path, dest: Path) {
        Files.walk(src).forEach {
            Files.copy(it, dest.resolve(src.relativize(it)),
                    StandardCopyOption.REPLACE_EXISTING)
        }
    }
}
