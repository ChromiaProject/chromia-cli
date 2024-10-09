package com.chromia.build.tools

import java.time.Duration
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.BlockDetail
import net.postchain.client.core.BlockRid
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.TransactionInfo
import net.postchain.client.core.TransactionResult
import net.postchain.client.core.TxRid
import net.postchain.client.core.Version
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.rest.HighestBlockHeightAnchoringCheck
import net.postchain.common.tx.TransactionStatus
import net.postchain.common.types.WrappedByteArray
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvPrimitive
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
    override fun blockAtHeight(height: Long): BlockDetail = TODO("Not yet implemented")
    override fun blockByRid(blockRid: BlockRid): BlockDetail = TODO("Not yet implemented")
    override fun awaitConfirmation(txRid: TxRid, retries: Int, pollInterval: Duration): TransactionResult =
            TransactionResult(txRid, TransactionStatus.CONFIRMED, null, null)

    override fun checkTxStatus(txRid: TxRid) = TODO("Not yet implemented")
    override fun close() = TODO("Not yet implemented")
    override fun confirmationProof(txRid: TxRid) = TODO("Not yet implemented")
    override fun currentBlockHeight(container: String?) = blockHeight()
    override fun getTransaction(txRid: TxRid) = TODO("Not yet implemented")
    override fun getTransactionInfo(txRid: TxRid): TransactionInfo = TODO("Not yet implemented")
    override fun getTransactionsCount(): Long = TODO("Not yet implemented")
    override fun getBlockchainRID(chainIID: Long): BlockchainRid = TODO("Not yet implemented")
    override fun getConfiguration(height: Long?): Gtv = TODO("Not yet implemented")

    override fun getHighestBlockHeightAnchoringCheck(): HighestBlockHeightAnchoringCheck =
            TODO("Not yet implemented")

    override fun getTransactionsInfo(limit: Long, beforeTime: Long, signer: String?): List<TransactionInfo> = TODO("Not yet implemented")
    override fun getVersion(): Version { TODO("Not yet implemented") }

    override fun postTransaction(tx: Gtx): TransactionResult =
            testConfiguration.txResultFactory(testConfiguration.txs.size).also { testConfiguration.txs.add(tx) }

    override fun postTransactionAwaitConfirmation(tx: Gtx): TransactionResult =
            testConfiguration.txResultFactory(testConfiguration.txs.size).also { testConfiguration.txs.add(tx) }

    override fun transactionBuilder() = TransactionBuilder(
            this, config.blockchainRid, config.signers.map { it.pubKey.data }, config.signers.map { it.sigMaker(Secp256K1CryptoSystem()) }
    )

    override fun transactionBuilder(signers: List<KeyPair>) = TODO("Not yet implemented")

    override fun query(name: String, args: Gtv): Gtv = when (name) {
        "api_version" -> gtv(28)
        "find_blockchain_rid" -> gtv(BlockchainRid.ZERO_RID.data)
        "get_container_data" -> gtv(getContainerDataResult(args["name"].toString()))
        "get_cluster_api_urls" -> gtv(listOf(gtv("http://node1_url"), gtv("http://node2_url")))
        "get_compressed_configuration_parts" -> gtv(listOf(args["configuration_part_hashes"]!![0]))
        "get_leases_by_account" -> gtv(listOf(gtv(getLeaseData()), gtv(getLeaseData())))
        "get_lease_by_container_name" -> gtv(getLeaseData())
        "get_economy_chain_rid" -> gtv(BlockchainRid.ZERO_RID.data)
        else -> TODO("Not yet implemented")
    }

    override fun validateConfiguration(configuration: Gtv) { }
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

fun getLeaseData(): Map<String, GtvPrimitive> {
    return mapOf(
            "container_name" to gtv("Container Name"),
            "cluster_name" to gtv("Cluster Name"),
            "container_units" to gtv(1),
            "extra_storage_gib" to gtv(1),
            "expire_time_millis" to gtv(50),
            "expired" to gtv(false),
            "auto_renew" to gtv(false),
            "subnode_image_name" to gtv("")
    )
}
