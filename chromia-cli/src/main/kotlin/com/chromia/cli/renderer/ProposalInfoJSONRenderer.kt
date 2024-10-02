package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.dataprovider.ProposalDataProviderFactory
import com.chromia.cli.tools.formatter.json
import com.chromia.directory1.model.Provider
import com.chromia.directory1.proposal.GetProposalResult
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.getProposalVotingResults
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient

class ProposalInfoJSONRenderer(val cliktCommand: CliktCommand) : Renderer<AbstractRenderData> {
    override fun display(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long, proposedBy: Provider) {
        val result = mutableMapOf<String, Any?>()
        result["proposal"] = proposal
        if (proposal.state == ProposalState.PENDING) {
            result["proposalStatus"] = client.getProposalVotingResults(proposal.id)
            result["proposalDescription"] = "Printing output information for proposal type: ${proposal.type} is not supported yet"
            val dataProvider = ProposalDataProviderFactory.getDataProvider(proposal.type, apiVersion)
            if (dataProvider != null) {
                result["proposalDescription"] = dataProvider.getData(client, proposal.id)
            }
            cliktCommand.echo(json(result))
        }
    }
}