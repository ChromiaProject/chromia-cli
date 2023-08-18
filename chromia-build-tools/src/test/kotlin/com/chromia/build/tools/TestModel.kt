package com.chromia.build.tools

import com.google.gson.JsonElement
import net.postchain.api.rest.BlockHeight
import net.postchain.api.rest.TransactionsCount
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.base.ConfirmationProof
import net.postchain.common.BlockchainRid
import net.postchain.core.BlockRid
import net.postchain.core.TransactionInfoExt
import net.postchain.core.block.BlockDetail
import net.postchain.ebft.rest.contract.StateNodeStatus
import net.postchain.gtv.Gtv
import net.postchain.gtx.GtxQuery

class TestModel(override val blockchainRid: BlockchainRid, override val chainIID: Long = 0) : Model {
    override var live: Boolean = true
    override fun debugQuery(subQuery: String?): JsonElement {
        TODO("Not yet implemented")
    }

    override fun getBlock(height: Long, txHashesOnly: Boolean): BlockDetail? {
        TODO("Not yet implemented")
    }

    override fun getBlock(blockRID: BlockRid, txHashesOnly: Boolean): BlockDetail? {
        TODO("Not yet implemented")
    }

    override fun getBlockchainConfiguration(height: Long): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getBlocks(beforeTime: Long, limit: Int, txHashesOnly: Boolean): List<BlockDetail> {
        TODO("Not yet implemented")
    }

    override fun getBlocksBeforeHeight(beforeHeight: Long, limit: Int, txHashesOnly: Boolean): List<BlockDetail> {
        TODO("Not yet implemented")
    }

    override fun getConfirmationProof(txRID: TxRid): ConfirmationProof? {
        TODO("Not yet implemented")
    }

    override fun getCurrentBlockHeight(): BlockHeight {
        TODO("Not yet implemented")
    }

    override fun getLastTransactionNumber(): TransactionsCount {
        TODO("Not yet implemented")
    }

    override fun getStatus(txRID: TxRid): ApiStatus {
        TODO("Not yet implemented")
    }

    override fun getTransaction(txRID: TxRid): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getTransactionInfo(txRID: TxRid): TransactionInfoExt? {
        TODO("Not yet implemented")
    }

    override fun getTransactionsInfo(beforeTime: Long, limit: Int): List<TransactionInfoExt> {
        TODO("Not yet implemented")
    }

    override fun nodePeersStatusQuery(): List<StateNodeStatus> {
        TODO("Not yet implemented")
    }

    override fun nodeStatusQuery(): StateNodeStatus {
        TODO("Not yet implemented")
    }

    override fun postTransaction(tx: ByteArray) {
        TODO("Not yet implemented")
    }

    override fun query(query: GtxQuery): Gtv {
        TODO("Not yet implemented")
    }

    override fun validateBlockchainConfiguration(configuration: Gtv) {
        TODO("Not yet implemented")
    }
}