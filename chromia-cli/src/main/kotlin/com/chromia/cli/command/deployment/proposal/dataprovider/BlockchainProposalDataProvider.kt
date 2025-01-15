package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.cli.d1.NopAnchoringGTXModule
import com.chromia.directory1.proposal_blockchain.GetBlockchainProposalResult
import com.chromia.directory1.proposal_blockchain.getBlockchainProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV1
import net.postchain.gtv.merkleHash

class BlockchainProposalDataProvider : ProposalDataProvider<GetBlockchainProposalResult> {
    private fun getDataHash(configData: WrappedByteArray) = GtvDecoder.decodeGtv(configData.data)
            .merkleHash(GtvMerkleHashCalculatorV1(NopAnchoringGTXModule.cryptoSystem))
            .wrap()

    override fun formatData(data: GetBlockchainProposalResult, cliktCommand: CliktCommand): Any {
        return "Container: ${data.container}\nConfig hash: ${getDataHash(data.data)}"
    }

    override fun getData(client: PostchainClient, proposalId: RowId): GetBlockchainProposalResult? {
        return client.getBlockchainProposal(proposalId)
    }
}