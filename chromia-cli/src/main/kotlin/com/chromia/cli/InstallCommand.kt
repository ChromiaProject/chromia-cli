package com.chromia.cli

import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.build.tools.lib.LibraryInstaller
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.libraryOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.validate
import kotlin.io.path.Path

class InstallCommand(
        private val repositoryClonerFactory: (quiet: Boolean) -> RepositoryCloner = { GitRepositoryCloner(quiet = it) },
) : CliktCommand(help = "Install library dependencies, if no library specified all will be installed") {
    private val settings by chromiaModelOption()
    private val library by libraryOption().multiple()
            .validate { require(settings.model.libs.keys.containsAll(it)) { "Specified library(s) $it does not exist in config file" } }

    override fun run() {
        val libraryInstaller = LibraryInstaller(
                repositoryClonerFactory(!terminal.info.outputInteractive),
                CliktCliEnv(this),
                Path(settings.sourceDir.absolutePath, InstallDirTarget.SOURCE.target),
                Path(settings.targetDir.absolutePath, InstallDirTarget.TEMP.target)
        )
        settings.model.libs.filter { library.isEmpty() || library.contains(it.key) }
                .forEach { libraryInstaller.installLibrary(it.key, it.value) }
    }
}
