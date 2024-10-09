package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import assertk.assertions.startsWith
import com.chromia.build.tools.TestClient
import com.chromia.cli.util.TestClusterManagement
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.common.BlockchainRid
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DeployInfoCommandTest {

    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    private val gson: Gson = GsonBuilder().create()

    @Test
    fun failedVerification(@TempDir dir: Path) {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }

        val settings = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  ok:
                    module: main
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
        assertBlockchainTableContainsRow("ok", "0000000000000000000000000000000000000000000000000000000000000002", "my_cluster")
        assertNodeTableContainsRow("http://myhost:7740", "569889", "OK")
        logger.clearOutput()

        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_found", "--network", "test"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 404 Not Found  Can't find blockchain from http://myhost:7740")
        logger.clearOutput()

        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "not_deployed", "--network", "test"))
        assertThat(logger.output()).contains("Cluster not found for blockchain rid 00:004")
        logger.clearOutput()

        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "has_errors", "--network", "test"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 500 Internal Server Error  Module initialization error from http://myhost:7740")
        logger.clearOutput()

        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test"))
        assertNodeTableContainsRow("http://myhost:7740", "570320", "OK")
        logger.clearOutput()

        command.parse(listOf("-s", settings.absolutePath, "--blockchain", "have_block", "--network", "test", "--verbose"))
        assertVerboseNodeTableContainsRow("http://myhost:7740", "119329", "HaveBlock", "2", "true", "OK")
    }

    @Test
    fun failedVerificationManualChain() {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }

        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--api-url", "http://myhost:7740"))
        assertBlockchainTableContainsRow("0000000000000000000000000000000000000000000000000000000000000002", "0000000000000000000000000000000000000000000000000000000000000002", "my_cluster")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000003", "--api-url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 404 Not Found  Can't find blockchain from http://myhost:7740")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000004", "--api-url", "http://myhost:7740"))
        assertThat(logger.output()).contains("Cluster not found for blockchain rid 00:004")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000005", "--api-url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 500 Internal Server Error  Module initialization error from http://myhost:7740")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--api-url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "570320", "OK")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--api-url", "http://myhost:7740", "--verbose"))
        assertVerboseNodeTableContainsRow("http://myhost:7740", "119329", "HaveBlock", "2", "true", "OK")
    }

    @Test
    fun tableOutput() {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--api-url", "http://myhost:7740", "--output-format", "table"))
        assertThat(logger.output()).contains("╭")
    }

    @Test
    fun jsonOutput() {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--api-url", "http://myhost:7740", "--output-format", "JSON"))
        assertThat(logger.output()).startsWith("{")
    }

    private fun assertBlockchainTableContainsRow(blockchain: String, blockchainRid: String, cluster: String) {
        val info = gson.fromJson(logger.output(), JsonObject::class.java)["blockchain"].asJsonObject
        assertThat(info["Blockchain"].asString).isEqualTo(blockchain)
        assertThat(info["Blockchain_Rid"].asString).isEqualTo(blockchainRid)
        assertThat(info["Cluster"].asString).isEqualTo(cluster)
    }

    private fun assertNodeTableContainsRow(nodeUrl: String, height: String, status: String) {
        val nodes = gson.fromJson(logger.output(), JsonObject::class.java)["nodes"].asJsonArray
        assertThat(nodes.contains(JsonObject().apply {
            addProperty("Node_url", nodeUrl)
            addProperty("Height", height)
            addProperty("Status", status)
        })).isTrue()
    }

    private fun assertVerboseNodeTableContainsRow(nodeUrl: String, height: String, state: String, round: String, revolting: String, status: String) {
        val nodes = gson.fromJson(logger.output(), JsonObject::class.java)["nodes"].asJsonArray
        assertThat(nodes.contains(JsonObject().apply {
            addProperty("Node_url", nodeUrl)
            addProperty("Height", height)
            addProperty("State", state)
            addProperty("Round", round)
            addProperty("Revolting", revolting)
            addProperty("Status", status)
        })).isTrue()
    }

    private fun testClientProvider(): PostchainClientProvider {
        return PostchainClientProvider {
            assertThat(it.endpointPool.size).isEqualTo(1)
            val endpoint = it.endpointPool.first()
            return@PostchainClientProvider when (it.blockchainRid) {
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002") -> TestClient(it, { 569889L })
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005") -> TestClient(it, { throw ClientError("Context", Status.INTERNAL_SERVER_ERROR, "Module initialization error", endpoint) })
                BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> TestClient(it, { 570320L })
                else -> TestClient(it, { throw ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", endpoint) })
            }
        }
    }

    private fun testClient(): (Request) -> Response = {
        assertThat(it.uri.host).contains("myhost")
        //assert(it.headers).contains("Accept" to "application/json") Can be PLAIN for /brid/iid_0
        when {
            it.uri.path.contains("brid/iid_0") -> Response(Status.OK).body("0000000000000000000000000000000000000000000000000000000000000001")
            it.uri.path.contains("06") -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":119329,\"serial\":159158134941,\"round\":2,\"blockRid\":\"328B9498981B459226B2E2B97B5E0AB4C0F4580D98E7C2F14DBA384FFFD90F80\",\"revolting\":true}")
            else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
        }
    }
}
