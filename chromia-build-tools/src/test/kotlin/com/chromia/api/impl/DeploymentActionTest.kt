package com.chromia.api.impl

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.chromia.build.tools.TestClient
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.testKeyPair
import com.chromia.cli.model.DeploymentModel
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.TransactionResult
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test

class DeploymentActionTest {

    val sentTxs = mutableListOf<Gtx>()
    @Test
    fun deploymentActionPostsTransaction() {
        val model = DeploymentModel(BlockchainRid.buildRepeat(1).wData, url = gtv("http://host"), container = null, chains = mapOf("first" to BlockchainRid.buildRepeat(2), "second" to BlockchainRid.buildRepeat(3)))
        val res = action(model, ChromiaClientConfig.EMPTY.setSigner(testKeyPair), BlockchainAction.pause, "For testint") { LoggedClient(it) }
        assertThat(res.first).isTrue()
        assertThat(res.second).isNull()
        assertThat(sentTxs.size).isEqualTo(1)
        val tx = sentTxs.first()
        assertThat(tx.signatures.size).isEqualTo(1)
        assertThat(tx.gtxBody.operations.size).isEqualTo(1 /* nop */ + 2 /* bc */)
    }

    inner class LoggedClient(config: PostchainClientConfig): TestClient(config, { 32 }) {
        override fun postTransactionAwaitConfirmation(tx: Gtx): TransactionResult {
            sentTxs.add(tx)
            return super.postTransactionAwaitConfirmation(tx)
        }
    }
}