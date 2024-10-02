package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.dataprovider.ProposalDataProviderFactory
import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.model.Provider
import com.chromia.directory1.proposal.GetProposalResult
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.ProposalVotingResults
import com.chromia.directory1.proposal.getProposalVoterInfo
import com.chromia.directory1.proposal.getProposalVotingResults
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.table.SectionBuilder
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId
import net.postchain.common.types.WrappedByteArray
import java.time.Instant
import java.util.*

class ProposalInfoTableRenderer(val cliktCommand: CliktCommand) : Renderer<AbstractRenderData> {
    private fun SectionBuilder.printProposalHeader(id: RowId, type: ProposalType, timestamp: Long, proposedByPubkey: WrappedByteArray, proposedByName: String) {
        row("Proposal", "${id.id} - ${type.name}")
        row("Proposed by", formatProvider(proposedByPubkey, proposedByName))
        row("Time", "${Date.from(Instant.ofEpochMilli(timestamp))}")
    }

    private fun formatProvider(providerPubKey: WrappedByteArray, providerName: String) =
            "${providerPubKey.toHex()}${if (providerName.isNotEmpty()) " - $providerName" else ""}"

    private fun SectionBuilder.printVotingInfo(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long) {
        if (proposal.state == ProposalState.PENDING) {
            printVotingResults(client.getProposalVotingResults(proposal.id))
        }

        if (apiVersion >= 9) {
            val votingInfo = client.getProposalVoterInfo(proposal.id)
            row("Providers that accepted", votingInfo.filter { it.vote }.joinToString("\n") { formatProvider(it.provider, it.providerName) })
            row("Providers that rejected", votingInfo.filterNot { it.vote }.joinToString("\n") { formatProvider(it.provider, it.providerName) })
        }
    }

    private fun SectionBuilder.printVotingResults(votingResults: ProposalVotingResults) {
        row("Positive votes", votingResults.positiveVotes.toString())
        row("Negative votes", votingResults.negativeVotes.toString())
        row("Max votes", votingResults.maxVotes.toString())
        row("Threshold", formatThreshold(votingResults.threshold))
        row("Status", votingResults.votingResult.toString())
    }

    override fun display(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long, proposedBy: Provider) {
        cliktCommand.echo(cliktCommand.defaultTable {
            body {
                printProposalHeader(proposal.id, proposal.type, proposal.timestamp, proposedBy.pubkey, proposedBy.name)
                row("State", proposal.state.toString())
                printVotingInfo(client, proposal, apiVersion)
                row("Description", proposal.description)
            }
        })
        if (proposal.state == ProposalState.PENDING) {
            if (cliktCommand.terminal.info.outputInteractive) cliktCommand.echo("Proposal details")
            val dataProvider = ProposalDataProviderFactory.getDataProvider(proposal.type, apiVersion)
            if (dataProvider != null) {
                cliktCommand.echo(dataProvider.getFormattedData(client, proposal.id, cliktCommand))
            } else {
                cliktCommand.echo("Printing output information for proposal type: ${proposal.type} is not supported yet")
            }
        }
    }
}