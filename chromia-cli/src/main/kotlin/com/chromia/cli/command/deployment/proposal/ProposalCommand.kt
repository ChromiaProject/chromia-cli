package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

/*
Implementation taken from: https://gitlab.com/chromaway/core-tools/management-console/-/tree/3.27.0/src/main/kotlin/net/postchain/mc/cli/proposal?ref_type=tags
 */
class ProposalCommand private constructor() : NoOpCliktCommand(help = "Act on proposals (Experimental)") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = ProposalCommand().subcommands(
                ProposalVoteCommand(),
                ProposalListCommand(),
                ProposalInfoCommand(),
                ProposalRevokeCommand()
        )

    }
}
