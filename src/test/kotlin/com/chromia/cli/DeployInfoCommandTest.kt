package com.chromia.cli

import assertk.assert
import assertk.assertions.contains
import com.chromia.cli.util.NodeStatusChecker
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import net.postchain.client.exception.ClientError
import net.postchain.client.request.Endpoint
import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DeployInfoCommandTest {
    @Test
    fun failedVerification(@TempDir dir: Path) {
        val testConsole = TestConsole()
        val command = DeployInfoCommand({ testClient() }, { TestClusterManagement() }).context { console = testConsole }

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
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "ok", "--target", "test"))
        assert(testConsole.out[0].first).contains("ok         | 00:002 | my_cluster")
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 569889 | OK")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_found", "--target", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 |        | Can't find blockchain")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_deployed", "--target", "test"))
        testConsole.assertContains("Cluster not found for blockchain rid 00:004\n")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "has_errors", "--target", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 |        | Module initialization error")
        testConsole.reset()
        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--target", "test"))
        assert(testConsole.out[1].first).contains("http://myhost:7740 | 570320 | OK")
    }

    private fun testClient(): (Request) -> Response = {
        when (it.uri.path) {
            "/node/0000000000000000000000000000000000000000000000000000000000000002/my_status" -> Response(Status.OK).body("{\"state\":\"WaitBlock\",\"height\":569889,\"serial\":159157543161,\"round\":0,\"revolting\":false}")
            "/node/0000000000000000000000000000000000000000000000000000000000000005/my_status" -> Response(Status.INTERNAL_SERVER_ERROR).body("{\"error\":\"Module initialization error\"}")
            "/node/0000000000000000000000000000000000000000000000000000000000000006/my_status" -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":570320,\"serial\":158306890607,\"round\":1,\"blockRid\":\"13F8AE0B71917DFCBB612600BEBA8F3AE1BB788AA23357AE8C62DF9D3FE9EAC0\",\"revolting\":false}")
            else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
        }
    }

    class TestClusterManagement: ClusterManagement {

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


