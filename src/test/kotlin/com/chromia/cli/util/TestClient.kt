package com.chromia.cli.util

import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.TransactionResult
import net.postchain.client.core.TxRid
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.tx.TransactionStatus
import net.postchain.crypto.KeyPair
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.Gtx
import java.time.Duration

class TestClient(override val config: PostchainClientConfig) : PostchainClient {
    val txs = mutableListOf<Gtx>()
    override fun blockAtHeight(height: Long) = TODO()
    override fun awaitConfirmation(txRid: TxRid, retries: Int, pollInterval: Duration) = TODO("Not yet implemented")
    override fun checkTxStatus(txRid: TxRid) = TODO("Not yet implemented")
    override fun close() = TODO("Not yet implemented")
    override fun confirmationProof(txRid: TxRid) = TODO("Not yet implemented")
    override fun currentBlockHeight() = TODO("Not yet implemented")
    override fun getTransaction(txRid: TxRid) = TODO("Not yet implemented")

    override fun postTransaction(tx: Gtx): TransactionResult {
        return TransactionResult(TxRid(""), TransactionStatus.CONFIRMED, null, null).also { txs.add(tx) }
    }

    override fun postTransactionAwaitConfirmation(tx: Gtx): TransactionResult {
        return TransactionResult(TxRid(""), TransactionStatus.CONFIRMED, null, null).also { txs.add(tx) }
    }

    override fun transactionBuilder() = TransactionBuilder(this, config.blockchainRid, listOf(), listOf())

    override fun transactionBuilder(signers: List<KeyPair>) = TODO("Not yet implemented")

    override fun query(name: String, args: Gtv): Gtv {
        return GtvFactory.gtv("")
    }
}