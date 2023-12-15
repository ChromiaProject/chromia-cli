package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate

class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val settings by chromiaModelOption()
    override fun aliases() = createAliases()

    override fun run() {
        if (settings.model.compile.library) {
            val libraryVerifyer = LibraryVerifyer(cliEnv())
            libraryVerifyer.calculateLibraryHashes(settings.model.compile.sourceFile(settings.projectFolder))
            return
        }
        ChromiaCompileApi.compile(cliEnv(), settings.model, settings.projectFolder)
    }
}
