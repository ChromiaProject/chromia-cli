package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.ProposalListRenderData
import com.chromia.cli.tools.formatter.defaultTable
import com.github.ajalt.clikt.core.CliktCommand

class ProposalListTableRenderer(val cliktCommand: CliktCommand) : Renderer<ProposalListRenderData> {
    override fun display(data: ProposalListRenderData) {
        cliktCommand.echo(cliktCommand.defaultTable {
            header { row("Type", "Id", "State", "Your vote") }
            body {
                data.proposals.map { info ->
                    val vote = data.votes.find { it.proposal == info.rowId }
                    val voteStatus = if (vote == null) "No vote registered" else if (vote.vote) "Accept" else "Reject"
                    row(info.proposalType.toString(), info.rowId.id.toString(), info.state.toString(), voteStatus)
                }
            }
        }
        )
    }
}
