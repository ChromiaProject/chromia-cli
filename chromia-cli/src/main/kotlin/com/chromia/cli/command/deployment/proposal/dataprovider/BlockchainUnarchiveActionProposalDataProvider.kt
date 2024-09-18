package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.GetBlockchainActionProposalResult
import com.chromia.directory1.proposal_blockchain.GetBlockchainUnarchiveActionProposalResult
import com.chromia.directory1.proposal_blockchain.getBlockchainActionProposal
import com.chromia.directory1.proposal_blockchain.getBlockchainUnarchiveActionProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId

class BlockchainUnarchiveActionProposalDataProvider : ProposalDataProvider<Pair<GetBlockchainActionProposalResult?, GetBlockchainUnarchiveActionProposalResult?>> {

    override fun formatData(data: Pair<GetBlockchainActionProposalResult?, GetBlockchainUnarchiveActionProposalResult?>, cliktCommand: CliktCommand): Any {
        val blockchainActionProposal = data.first
        val unarchivingProposal = data.second
        if (blockchainActionProposal == null) return ""
        return cliktCommand.defaultTable {
            body {
                row("Blockchain RID", blockchainActionProposal.blockchain.toHex())
                row("Blockchain name", blockchainActionProposal.blockchainName)
                row("Action", blockchainActionProposal.action.name)

                if (blockchainActionProposal.action == BlockchainAction.unarchive) {
                    if (unarchivingProposal != null) {
                        row("Source container", unarchivingProposal.sourceContainer)
                        row("Destination container", unarchivingProposal.destinationContainer)
                        row("Final height", unarchivingProposal.finalHeight)
                    }
                }
            }
        }
    }

    override fun getData(client: PostchainClient, proposalId: RowId): Pair<GetBlockchainActionProposalResult?, GetBlockchainUnarchiveActionProposalResult?> {
        val action = client.getBlockchainActionProposal(proposalId)
        return if (action != null && action.action == BlockchainAction.unarchive) {
            Pair(action, client.getBlockchainUnarchiveActionProposal(proposalId))
        } else {
            Pair(action, null)
        }
    }
}