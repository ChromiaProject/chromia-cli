package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.tools.formatter.chromiaTheme
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.TestClient
import com.chromia.cli.util.TestClusterManagement
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.table.SectionBuilder
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
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

    private val logger = TerminalRecorder(width = 1000)
    private val testTerminal = Terminal(logger)

    @Test
    fun failedVerification(@TempDir dir: Path) {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }

        val settings = File(dir.toFile(), "chromia.yml").apply {
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
        assertBlockchainTableContainsRow("ok", "00:002", "my_cluster")
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
    fun failedVerificationManualChain(@TempDir dir: Path) {
        val command = DeployInfoCommand(testClientProvider(), { TestClusterManagement() }, { testClient() }).context { terminal = testTerminal }

        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--url", "http://myhost:7740"))
        assertBlockchainTableContainsRow("0000000000000000000000000000000000000000000000000000000000000002", "00:002", "my_cluster")
        assertThat(logger.output()).contains("0000000000000000000000000000000000000000000000000000000000000002") // Make sure we don't cut the line lenght
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000003", "--url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 404 Not Found  Can't find blockchain from http://myhost:7740")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000004", "--url", "http://myhost:7740"))
        assertThat(logger.output()).contains("Cluster not found for blockchain rid 00:004")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000005", "--url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "-1", "Context: 500 Internal Server Error  Module initialization error from http://myhost:7740")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740"))
        assertNodeTableContainsRow("http://myhost:7740", "570320", "OK")
        logger.clearOutput()
        command.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--url", "http://myhost:7740", "--verbose"))
        assertVerboseNodeTableContainsRow("http://myhost:7740", "119329", "HaveBlock", "2", "true", "OK")
    }

    private fun assertBlockchainTableContainsRow(vararg row: String) {
        assertTableContainsRow({ row("Blockchain", "Rid", "Cluster") }, { row(row) })
    }

    private fun assertNodeTableContainsRow(vararg row: String) {
        assertTableContainsRow({ row("Node url", "Height", "Status") }, { row(row) })
    }

    private fun assertVerboseNodeTableContainsRow(vararg row: String) {
        assertTableContainsRow({ row("Node url", "Height", "State", "Round", "Revolting", "Status") }, { row(row) })
    }

    private fun assertTableContainsRow(headerRow: SectionBuilder.() -> Unit, bodyRow: SectionBuilder.() -> Unit) {
        val recorder = TerminalRecorder()
        val testTerminal = Terminal(recorder)
        chromiaTheme.defaultTable {
            header(headerRow)
            body(bodyRow)
        }.render(testTerminal)
        assertThat(logger.output()).contains(recorder.output())
    }


    private fun testClientProvider(): PostchainClientProvider {
        return PostchainClientProvider {
            assertThat(it.endpointPool.size).equals(1)
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


