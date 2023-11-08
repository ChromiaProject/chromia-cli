package com.chromia.cli

import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class EifCommand : NoOpCliktCommand(help = "Ethereum Integration Framework commands") {
    override fun aliases() = createAliases()
}

fun eifCommands() = EifCommand().subcommands(
        EIFGenerateEventsConfigCommand()
)
