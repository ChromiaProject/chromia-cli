package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.cli.tools.config.ChromiaModelOption
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.CliktCliEnv
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate

class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val settings by ChromiaModelOption()
    override fun aliases() = createAliases()

    override fun run() {
        ChromiaCompileApi.compile(CliktCliEnv(this), settings.model, settings.modelFile.parentFile)
    }
}
