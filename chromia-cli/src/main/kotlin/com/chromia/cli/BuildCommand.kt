package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter


class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val settings by settingsOption()
    override fun aliases() = createAliases()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        ChromiaCompileApi.compile(CliktCliEnv(this), settings.model, settings.file.parentFile)
    }
}
