package com.chromia.cli.command

import com.chromia.api.ChromiaLibrariesApi
import com.chromia.api.filterLibraries
import com.chromia.build.tools.lib.GitRepositoryCloner
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.DependencyUpdatedMarker
import com.chromia.cli.util.libraryOption
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.validate

// TODO: Remove this command
class InstallCommand(
        private val repositoryClonerFactory: (quiet: Boolean) -> RepositoryCloner = { GitRepositoryCloner(quiet = it) },
) : ChromiaCommand(help = "Install library dependencies, if no library specified all will be installed") {
    private val settings by chromiaModelOption()
    private val library by libraryOption().multiple()
            .validate { require(settings.model.libs.keys.containsAll(it)) { "Specified library(s) $it does not exist in config file" } }

    private val dependencyUpdatedMarker by lazy { DependencyUpdatedMarker(settings.targetDir) }

    override fun run() {
        ChromiaLibrariesApi.install(
                CliktCliEnv(this),
                settings.model.filterLibraries(library.takeIf { it.isNotEmpty() }),
                repositoryClonerFactory(!terminal.terminalInfo.outputInteractive),
        )

        dependencyUpdatedMarker.markAsInstalled()
    }
}
