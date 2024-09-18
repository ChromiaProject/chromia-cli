package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.proposal_blockchain_move.GetBlockchainMoveFinishProposalResult
import com.chromia.directory1.proposal_blockchain_move.getBlockchainMoveFinishProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId

class BlockchainMoveFinishProposalDataProvider : ProposalDataProvider<GetBlockchainMoveFinishProposalResult> {

    override fun formatData(data: GetBlockchainMoveFinishProposalResult, cliktCommand: CliktCommand): Any {
        return cliktCommand.defaultTable {
            body {
                row("Blockchain RID", data.blockchainRid)
                row("Blockchain name", data.blockchainName)
                row("Cluster", data.cluster)
                row("Container", data.container)
                row("Final height", data.finalHeight)
            }
        }
    }

    override fun getData(client: PostchainClient, proposalId: RowId): GetBlockchainMoveFinishProposalResult? {
        return client.getBlockchainMoveFinishProposal(proposalId)
    }
}