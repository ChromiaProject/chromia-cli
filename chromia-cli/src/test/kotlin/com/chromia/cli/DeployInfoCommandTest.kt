package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.BlockDetail
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.TransactionResult
import net.postchain.client.core.TxRid
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.crypto.KeyPair
import net.postchain.d1.cluster.ClusterManagement
import net.postchain.gtv.Gtv
import net.postchain.gtx.Gtx
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration

class DeployInfoCommandTest {
    @Test
    fun failedVerification(@TempDir dir: Path) {
        val testConsole = TestConsole()
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { console = testConsole }

        val settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                deployments:
                  test:
                    url: "localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    chains:
                      ok:        x"0000000000000000000000000000000000000000000000000000000000000002"
                      not_found:    x"0000000000000000000000000000000000000000000000000000000000000003"
                      not_deployed: x"0000000000000000000000000000000000000000000000000000000000000004"
                      has_errors:   x"0000000000000000000000000000000000000000000000000000000000000005"
                      have_block:   x"0000000000000000000000000000000000000000000000000000000000000006"
            """.trimIndent())
        }
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "ok", "--network", "test"))
        assertThat(testConsole.out[0].first).contains("ok         | 00:002 | my_cluster")
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 569889 | OK")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_found", "--network", "test"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 404 Not Found  Can't find blockchain from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_deployed", "--network", "test"))
        testConsole.assertContains("Cluster not found for blockchain rid 00:004")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "has_errors", "--network", "test"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 500 Internal Server Error  Module initialization error from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 570320 | OK")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test", "--verbose"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 119329 | HaveBlock | 2     | true      | OK")
    }

    @Test
    fun failedVerificationManualChain(@TempDir dir: Path) {
        val testConsole = TestConsole()
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { console = testConsole }

        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--url", "http://myhost:7740"))
        assertThat(testConsole.out[0].first).contains("0000000000000000000000000000000000000000000000000000000000000002 | 00:002 | my_cluster")
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 569889 | OK")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000003", "--url", "http://myhost:7740"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 404 Not Found  Can't find blockchain from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000004", "--url", "http://myhost:7740"))
        testConsole.assertContains("Cluster not found for blockchain rid 00:004")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000005", "--url", "http://myhost:7740"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 500 Internal Server Error  Module initialization error from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 570320 | OK")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740", "--verbose"))
        assertThat(testConsole.out[1].first).contains("http://myhost:7740 | 119329 | HaveBlock | 2     | true      | OK")
    }


    private fun testClientProvider(): PostchainClientProvider {
        return PostchainClientProvider {
            assertThat(it.endpointPool.size).equals(1)
            val endpoint = it.endpointPool.first()
            return@PostchainClientProvider when (it.blockchainRid) {
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002") -> TestPostchainClient(it) { 569889L }
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005") -> TestPostchainClient(it) { throw ClientError("Context", Status.INTERNAL_SERVER_ERROR, "Module initialization error", endpoint) }
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> TestPostchainClient(it) { 570320L }
                else -> TestPostchainClient(it) { throw ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", endpoint) }
            }
        }
    }

    private fun testClient(): (Request) -> Response = {
        assertThat(it.uri.host).isEqualTo("myhost")
        //assert(it.headers).contains("Accept" to "application/json") Can be PLAIN for /brid/iid_0
        when {
            it.uri.path.contains("brid/iid_0") -> Response(Status.OK).body("0000000000000000000000000000000000000000000000000000000000000001")
            it.uri.path.contains("06") -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":119329,\"serial\":159158134941,\"round\":2,\"blockRid\":\"328B9498981B459226B2E2B97B5E0AB4C0F4580D98E7C2F14DBA384FFFD90F80\",\"revolting\":true}")
            else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
        }
    }

    class TestPostchainClient(
            override val config: PostchainClientConfig,
            val blockHeight: () -> Long
    ) : PostchainClient {
        override fun currentBlockHeight() = blockHeight()
        override fun close() {}
        override fun transactionBuilder(): TransactionBuilder = TODO()
        override fun transactionBuilder(signers: List<KeyPair>) = TODO()
        override fun query(name: String, args: Gtv): Gtv = TODO()
        override fun postTransaction(tx: Gtx): TransactionResult = TODO()
        override fun checkTxStatus(txRid: TxRid): TransactionResult = TODO()
        override fun postTransactionAwaitConfirmation(tx: Gtx): TransactionResult = TODO()
        override fun awaitConfirmation(txRid: TxRid, retries: Int, pollInterval: Duration): TransactionResult = TODO()
        override fun confirmationProof(txRid: TxRid): ByteArray = TODO()
        override fun blockAtHeight(height: Long): BlockDetail? = TODO()
        override fun getTransaction(txRid: TxRid): ByteArray = TODO()
    }

    class TestClusterManagement : ClusterManagement {

        override fun getClusterOfBlockchain(blockchainRid: BlockchainRid) = if (!blockchainRid.toHex().endsWith("4")) "my_cluster" else throw ClientError("", Status(404, null), "", Endpoint(""))
        override fun getBlockchainApiUrls(blockchainRid: BlockchainRid) = listOf("http://myhost:7740")
        override fun getActiveBlockchains(clusterName: String) = TODO("Not yet implemented")
        override fun getBlockchainPeers(blockchainRid: BlockchainRid, height: Long) = TODO("Not yet implemented")
        override fun getClusterInfo(clusterName: String) = TODO("Not yet implemented")
        override fun getClusterNames() = TODO("Not yet implemented")
        override fun getClusterAnchoringChains() = TODO("Not yet implemented")
        override fun getSystemAnchoringChain() = TODO("Not yet implemented")
    }
}


