package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.directory1.proposal_blockchain.GetBlockchainProposalResult
import com.chromia.directory1.proposal_blockchain.getBlockchainProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.base.configuration.BlockchainConfigurationData
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId
import net.postchain.gtv.GtvDecoder.decodeGtv

class BlockchainProposalDataProvider : ProposalDataProvider<GetBlockchainProposalResult> {

    override fun formatData(data: GetBlockchainProposalResult, cliktCommand: CliktCommand): Any {
        return "Container: ${data.container}\nConfig hash: ${BlockchainConfigurationData.merkleHash(decodeGtv(data.data.data))}"
    }

    override fun getData(client: PostchainClient, proposalId: RowId): GetBlockchainProposalResult? {
        return client.getBlockchainProposal(proposalId)
    }
}