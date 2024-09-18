package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.proposal_blockchain.GetBlockchainActionProposalResult
import com.chromia.directory1.proposal_blockchain.getBlockchainActionProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId

class BlockchainActionProposalDataProvider : ProposalDataProvider<GetBlockchainActionProposalResult> {

    override fun formatData(data: GetBlockchainActionProposalResult, cliktCommand: CliktCommand): Any {
        return cliktCommand.defaultTable {
            body {
                row("Blockchain RID", data.blockchain.toHex())
                row("Blockchain name", data.blockchainName)
                row("Action", data.action.name)
            }
        }
    }

    override fun getData(client: PostchainClient, proposalId: RowId): GetBlockchainActionProposalResult? {
        return client.getBlockchainActionProposal(proposalId)
    }
}