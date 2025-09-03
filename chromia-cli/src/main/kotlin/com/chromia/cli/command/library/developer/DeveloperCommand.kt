package com.chromia.cli.command.library.developer

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class DeveloperCommand : NoOpChromiaCommand(help = "Manage library developers") {
    override val hiddenFromHelp = true
    companion object {
        fun commands() = DeveloperCommand().subcommands(
            CreateDeveloperCommand(),
            GetDeveloperCommand(),
            UpdateDeveloperCommand()
        )
    }
}
