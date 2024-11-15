package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class VotersetCommand : NoOpChromiaCommand(help = "Interact with votersets") {

    companion object {
        fun commands() = VotersetCommand().subcommands(
                VotersetInfoCommand(),
                VotersetUpdateCommand(),
                VotersetListCommand(),
                VotersetAddDappProviderCommand()
        )

    }
}
