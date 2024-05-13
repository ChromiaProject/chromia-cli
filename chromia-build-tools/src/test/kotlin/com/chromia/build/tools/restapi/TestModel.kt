package com.chromia.build.tools.restapi

import net.postchain.api.rest.BlockHeight
import net.postchain.api.rest.BlockSignature
import net.postchain.api.rest.BlockchainNodeState
import net.postchain.api.rest.TransactionsCount
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.base.ConfirmationProof
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.NotFound
import net.postchain.common.tx.TransactionStatus
import net.postchain.core.BlockRid
import net.postchain.core.TransactionInfoExt
import net.postchain.core.block.BlockDetail
import net.postchain.crypto.PubKey
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.ebft.rest.contract.StateNodeStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtx.Gtx
import net.postchain.gtx.GtxQuery

class TestModel(
        override val blockchainRid: BlockchainRid = BlockchainRid.buildRepeat(1),
        override val chainIID: Long = 0,
) : CachedModel {
    override val queryCacheTtlSeconds: Long = 0
    override var live: Boolean = true
    override val txQueue = mutableListOf<Gtx>()
    override val txMap = mutableMapOf<TxRid, Gtx>()
    override fun confirmBlock(blockRID: BlockRid): BlockSignature? {
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

    override fun getBlockchainNodeState(): BlockchainNodeState {
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

    override fun getNextBlockchainConfigurationHeight(height: Long): BlockHeight? {
        TODO("Not yet implemented")
    }

    override fun getStatus(txRID: TxRid): ApiStatus {
        val tx = txMap[txRID]
        return ApiStatus(tx?.let { TransactionStatus.CONFIRMED } ?: TransactionStatus.UNKNOWN)
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

    override fun getTransactionsInfoBySigner(beforeTime: Long, limit: Int, signer: PubKey): List<TransactionInfoExt> {
        TODO("Not yet implemented")
    }

    override fun nodePeersStatusQuery(): List<StateNodeStatus> {
        TODO("Not yet implemented")
    }

    override fun nodeStatusQuery(): StateNodeStatus {
        TODO("Not yet implemented")
    }

    override fun postTransaction(tx: ByteArray) {
        val decoded = Gtx.decode(tx)
        val rid = decoded.gtxBody.calculateTxRid(GtvMerkleHashCalculator(Secp256K1CryptoSystem())).let { TxRid(it) }
        txQueue.add(decoded)
        txMap[rid] = decoded
    }

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "rell.get_rell_version" -> gtv("0.13.11")
            else -> throw NotFound("Query ${query.name} no implemented")
        }
    }

    override fun validateBlockchainConfiguration(configuration: Gtv) {
        TODO("Not yet implemented")
    }
}
