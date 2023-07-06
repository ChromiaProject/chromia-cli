package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand

class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val settings by settingsOption()
    override fun aliases() = createAliases()

    override fun run() {
        ChromiaCompileApi.compile(CliktCliEnv(this), settings.model, settings.file.parentFile)
    }
}
