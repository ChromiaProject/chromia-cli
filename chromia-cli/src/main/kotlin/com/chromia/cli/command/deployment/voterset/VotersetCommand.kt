package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class VotersetCommand() : NoOpCliktCommand(help = "Interact with votersets (Experimental)") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = VotersetCommand().subcommands(
                VotersetInfoCommand(),
                VotersetUpdateCommand(),
                VotersetListCommand()
        )

    }
}
