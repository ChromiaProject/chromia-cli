package com.chromia.cli.command.tools

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class ToolsCommand : NoOpChromiaCommand(help = "Miscellaneous tools") {

    companion object {
        fun commands() = ToolsCommand().subcommands(
                GtvCommand(),
                ValidateModelCommand(),
                LibModelGenerateCommand()
        )
    }

}
