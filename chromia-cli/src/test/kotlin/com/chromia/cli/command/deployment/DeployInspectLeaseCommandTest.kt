package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.util.TestClient
import com.chromia.build.tools.testData
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class DeployInspectLeaseCommandTest {
    private val logger = TerminalRecorder(width = 1000)
    private val testTerminal = Terminal(logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        testData(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @Test
    fun `Getting lease with containerId from network config`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    url:
                      - http://localhost:7740
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000000"
        """.trimIndent())

        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }
        command.parse(listOf("--network", "test_network", "-s", settingsFile.absolutePath))
        assertThat(logger.output()).contains("Getting lease information for container: containerId")
    }

    @Test
    fun `Missing containerId from network config`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    url:
                      - http://localhost:7740
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000000"
        """.trimIndent())

        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }
        val res = assertThrows<Exception> {
            command.parse(listOf("--network", "test_network", "-s", settingsFile.absolutePath))
        }
        assertThat(res.message).isEqualTo("Option pubkey or container name needs to be specified.")
    }

    @Test
    fun `Setting container Id explicitly takes precedence over configured container id`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    url:
                      - http://localhost:7740
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000000"
        """.trimIndent())
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }
        command.parse(listOf("--network", "test_network", "--container-id", "A Different Container", "-s", settingsFile.absolutePath))
        assertThat(logger.output()).contains("Getting lease information for container: A Different Container")
    }

    @Test
    fun `Pubkey takes precedence over container id`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    url:
                      - http://localhost:7740
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000000"
        """.trimIndent())
        val pubkey = "0000000000000000000000000000000000000000000000000000000000000003"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }
        command.parse(listOf("--network", "test_network", "--pubkey", pubkey, "-s", settingsFile.absolutePath))
        assertThat(logger.output()).contains("Getting active leases for user with public key: $pubkey")
    }

    @Test
    fun `Explicit setting system option`() {
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }

        val res = assertThrows<Exception> {
            command.parse(listOf("--url", "http://localhost:7740", "--blockchain-rid", brid))
        }
        assertThat(res.message).isEqualTo("Option pubkey or container name needs to be specified.")
    }

    @Test
    fun `Explicit setting system option using pubkey to get lease data`() {
        val pubkey = "03DD65032C7BEE117FFDAA94C8DADAE79ADBA2A3A17C8D4A5239AA2DC2E93D845E"
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }

        command.parse(listOf("--url", "http://localhost:7740", "--blockchain-rid", brid, "--pubkey", pubkey))
        assertThat(logger.output()).contains("Getting active leases for user with public key: $pubkey")
    }

    @Test
    fun `Explicit setting system option using container id to get lease data`() {
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }

        command.parse(listOf("--url", "http://localhost:7740", "--blockchain-rid", brid, "--container-id", "container-id"))
        assertThat(logger.output()).contains("Getting lease information for container: container-id")
    }

    @Test
    fun `Invalid public key with illegal characters`() {
        val pubkey = "000000000000000000000000000000000000000000000000000000000000000G"
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }

        val res = assertThrows<Exception> {
            command.parse(listOf("--url", "http://localhost:7740", "--blockchain-rid", brid, "--pubkey", pubkey))
        }
        assertThat(res.message.toString()).contains("Public key contains one ore more illegal character. Supported Characters are: 0-9, A-F, a-f.")
    }

    @Test
    fun `Invalid public key of uneven length`() {
        val pubkey = "00000000000000000000000000000000000000000000000000000000000000034"
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand { TestClient(it, { 0 }) }.context { terminal = testTerminal }

        val res = assertThrows<Exception> {
            command.parse(listOf("--url", "http://localhost:7740", "--blockchain-rid", brid, "--pubkey", pubkey))
        }
        assertThat(res.message.toString()).contains("The public key must be a hex string with even length. Length was: ${pubkey.length}")
    }
}
