package com.chromia.cli

import assertk.assert
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import java.io.File
import java.nio.file.Path

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
        assert(testConsole.out[0].first).contains("ok         | 00:002 | my_cluster")
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 569889 | OK")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_found", "--network", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 404 Not Found Can't find blockchain from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_deployed", "--network", "test"))
        testConsole.assertContains("Cluster not found for blockchain rid 00:004\n")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "has_errors", "--network", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 500 Internal Server Error Module initialization error from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 570320 | OK")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test", "--verbose"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 119329 | HaveBlock | 2     | true      | OK")
    }

    @Test
    fun failedVerificationManualChain(@TempDir dir: Path) {
        val testConsole = TestConsole()
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { console = testConsole }

        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--url", "http://myhost:7740"))
        assert(testConsole.out[0].first).contains("0000000000000000000000000000000000000000000000000000000000000002 | 00:002 | my_cluster")
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 569889 | OK")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000003", "--url", "http://myhost:7740"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 404 Not Found Can't find blockchain from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000004", "--url", "http://myhost:7740"))
        testConsole.assertContains("Cluster not found for blockchain rid 00:004\n")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000005", "--url", "http://myhost:7740"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | -1     | Context: 500 Internal Server Error Module initialization error from http://myhost:7740")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 570320 | OK")
        testConsole.reset()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740", "--verbose"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 119329 | HaveBlock | 2     | true      | OK")
    }


    private fun testClientProvider(): PostchainClientProvider {
        return PostchainClientProvider {
            assert(it.endpointPool.size).equals(1)
            val endpoint = it.endpointPool.first()
            return@PostchainClientProvider when (it.blockchainRid) {
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002") -> mock { on { currentBlockHeight() } doReturn 569889L }
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005") -> mock { on { currentBlockHeight() } doThrow ClientError("Context", Status.INTERNAL_SERVER_ERROR, "Module initialization error", endpoint) }
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> mock { on { currentBlockHeight() } doReturn 570320L }
                else -> mock { on { currentBlockHeight() } doThrow ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", endpoint) }
            }
        }
    }

    private fun testClient(): (Request) -> Response = {
        assert(it.uri.host).isEqualTo("myhost")
        assert(it.headers).contains("Accept" to "application/json")
        when {
            it.uri.path.contains("06") -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":119329,\"serial\":159158134941,\"round\":2,\"blockRid\":\"328B9498981B459226B2E2B97B5E0AB4C0F4580D98E7C2F14DBA384FFFD90F80\",\"revolting\":true}")
            else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
        }
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


