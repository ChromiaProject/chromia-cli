package com.chromia.cli.util

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.TransactionInfo
import net.postchain.client.core.TransactionResult
import net.postchain.client.core.TxRid
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.KeyPair
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvPrimitive
import net.postchain.gtx.Gtx
import java.time.Duration

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
        "get_container_data" -> gtv(getContainerDataResult(args["name"].toString()))
        "get_cluster_api_urls" -> gtv(listOf(gtv("http://node1_url"), gtv("http://node2_url")))
        else -> TODO("Not yet implemented")
    }
}

fun getContainerDataResult(name: String): Map<String, GtvPrimitive> {
    val pubkey = WrappedByteArray.fromHex("0000000000000000000000000000000000000000000000000000000000000001")
    return mapOf(
            "name" to gtv(name),
            "cluster" to gtv("cluster"),
            "deployer" to gtv("deployer"),
            "proposed_by_pubkey" to gtv(pubkey),
            "proposed_by_name" to gtv("proposed_by_name"),
            "system" to gtv(false)
    )
}
