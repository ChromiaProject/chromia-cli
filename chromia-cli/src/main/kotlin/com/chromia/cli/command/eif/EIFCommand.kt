package com.chromia.cli.command.eif

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.subcommands

class EifCommand private constructor(): NoOpChromiaCommand(help = "Ethereum Integration Framework commands") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = EifCommand().subcommands(
                EIFGenerateEventsConfigCommand()
        )
    }
}
