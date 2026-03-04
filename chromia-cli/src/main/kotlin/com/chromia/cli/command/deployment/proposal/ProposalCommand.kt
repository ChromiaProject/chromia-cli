package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

/*
Implementation taken from: https://gitlab.com/chromaway/core-tools/management-console/-/tree/3.27.0/src/main/kotlin/net/postchain/mc/cli/proposal?ref_type=tags
 */
class ProposalCommand private constructor() : NoOpChromiaCommand(help = "Act on proposals") {
    companion object {
        fun commands() = ProposalCommand().subcommands(
                ProposalVoteCommand(),
                ProposalRetractVoteCommand(),
                ProposalListCommand(),
                ProposalInfoCommand(),
                ProposalRevokeCommand(),
                ProposalRenameBlockchainCommand()
        )

    }
}
