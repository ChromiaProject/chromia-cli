package com.chromia.cli.util

import java.time.Duration
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.TransactionInfo
import net.postchain.client.core.TransactionResult
import net.postchain.client.core.TxRid
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.crypto.KeyPair
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.Gtx

data class TestConfiguration(
        val txs: MutableList<Gtx> = mutableListOf(),
        val txResultFactory: (n: Int) -> TransactionResult = { TransactionResult(TxRid(""), TransactionStatus.CONFIRMED, null, null) },
)

open class TestClient(
        override val config: PostchainClientConfig,
        val blockHeight: () -> Long,
        private val testConfiguration: TestConfiguration = TestConfiguration()
) : PostchainClient {
    override fun blockAtHeight(height: Long) = TODO()
    override fun awaitConfirmation(txRid: TxRid, retries: Int, pollInterval: Duration): TransactionResult =
            TransactionResult(txRid, TransactionStatus.CONFIRMED, null, null)

    override fun checkTxStatus(txRid: TxRid) = TODO("Not yet implemented")
    override fun close() = TODO("Not yet implemented")
    override fun confirmationProof(txRid: TxRid) = TODO("Not yet implemented")
    override fun currentBlockHeight() = blockHeight()
    override fun getTransaction(txRid: TxRid) = TODO("Not yet implemented")
    override fun getTransactionInfo(txRid: TxRid): TransactionInfo = TODO("Not yet implemented")
    override fun getTransactionsCount(): Long = TODO("Not yet implemented")
    override fun getTransactionsInfo(limit: Long, beforeTime: Long, signer: String?): List<TransactionInfo> = TODO("Not yet implemented")

    override fun postTransaction(tx: Gtx): TransactionResult =
            testConfiguration.txResultFactory(testConfiguration.txs.size).also { testConfiguration.txs.add(tx) }

    override fun postTransactionAwaitConfirmation(tx: Gtx): TransactionResult =
            testConfiguration.txResultFactory(testConfiguration.txs.size).also { testConfiguration.txs.add(tx) }

    override fun transactionBuilder() = TransactionBuilder(
            this, config.blockchainRid, listOf(), listOf()
    )

    override fun transactionBuilder(signers: List<KeyPair>) = TODO("Not yet implemented")

    override fun query(name: String, args: Gtv): Gtv = when (name) {
        "api_version" -> gtv(8)
        "find_blockchain_rid" -> gtv(BlockchainRid.ZERO_RID.data)
        else -> TODO("Not yet implemented")
    }
}
