package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.ProposalListRenderData
import com.chromia.cli.tools.formatter.json
import com.github.ajalt.clikt.core.CliktCommand

class ProposalListJSONRenderer(val cliktCommand: CliktCommand) : Renderer<ProposalListRenderData> {
    override fun display(data: ProposalListRenderData) {
        val result = mutableMapOf<String, Any?>()


        data.proposals.map { info ->
            val vote = data.votes.find { it.proposal == info.rowId }
            val voteStatus = if (vote == null) "No vote registered" else if (vote.vote) "Accept" else "Reject"
            result[info.rowId.id.toString()] =
                    ProposalInfo(info.proposalType.toString(), info.rowId.id.toString(), info.state.toString(), voteStatus)
        }
        cliktCommand.echo(json(result))
    }

    private data class ProposalInfo(val type: String, val id: String, val state: String, val status: String)
}
