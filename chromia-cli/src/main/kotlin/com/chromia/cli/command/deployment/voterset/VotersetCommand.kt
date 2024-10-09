package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.subcommands

class VotersetCommand : NoOpChromiaCommand(help = "Interact with votersets") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = VotersetCommand().subcommands(
                VotersetInfoCommand(),
                VotersetUpdateCommand(),
                VotersetListCommand(),
                VotersetAddDappProviderCommand()
        )

    }
}
