package com.chromia.cli.command.tools

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.subcommands

class ToolsCommand : NoOpChromiaCommand(help = "Miscellaneous tools") {

    override fun aliases() = createAliases()

    companion object {
        fun commands() = ToolsCommand().subcommands(
                GtvCommand(),
        )
    }

}
