package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.getLeaseData
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQuery
import com.chromia.build.tools.testData
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import java.io.File
import java.nio.file.Path
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory.gtv
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

class DeployInspectLeaseCommandTest {
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    private val directoryChainModel = DirectoryChainModel().withQuery("get_economy_chain_rid", gtv(BlockchainRid.buildRepeat(1)))
    private val economyChainModel = TestModel(BlockchainRid.buildRepeat(1), 1)

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
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
        """.trimIndent())

        withModel(directoryChainModel, economyChainModel.withQuery("get_lease_by_container_name", gtv(getLeaseData()))) {
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }
            command.parse(listOf("--network", "test_network", "-s", settingsFile.absolutePath))
            assertThat(logger.output()).contains(""""container": "containerId"""")
        }
    }

    @Test
    fun `Missing containerId from network config`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
        """.trimIndent())

        withModel() {
            val command = DeployInspectLeaseCommand()
            val res = assertThrows<Exception> {
                command.parse(listOf("--network", "test_network", "-s", settingsFile.absolutePath))
            }
            assertThat(res.message).isEqualTo("Option pubkey or container name needs to be specified.")
        }
    }

    @Test
    fun `Setting container Id explicitly takes precedence over configured container id`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - http://localhost:7745
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
        """.trimIndent())
        withModel(directoryChainModel, economyChainModel.withQuery("get_lease_by_container_name", gtv(getLeaseData()))) {
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }
            command.parse(listOf("--network", "test_network", "--container-id", "A Different Container", "-s", settingsFile.absolutePath))
            assertThat(logger.output()).contains(""""container": "A Different Container"""")
        }
    }

    @Test
    fun `Pubkey takes precedence over container id`() {
        settingsFile.appendText("""
            \n
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    container: containerId
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
        """.trimIndent())
        withModel(directoryChainModel, economyChainModel.withQuery(
                "get_leases_by_account", gtv(listOf(gtv(getLeaseData()), gtv(getLeaseData())))
        )) {
            val pubkey = "0000000000000000000000000000000000000000000000000000000000000003"
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }
            command.parse(listOf("--network", "test_network", "--pubkey", pubkey, "-s", settingsFile.absolutePath))
            assertThat(logger.output()).contains(""""pubkey": "$pubkey"""")
        }
    }

    @Test
    fun `Explicit setting system option`() {
        withModel(directoryChainModel, economyChainModel) {
            val brid = "0000000000000000000000000000000000000000000000000000000000000000"
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }

            val res = assertThrows<Exception> {
                command.parse(listOf("--api-url", "http://localhost:7745", "--blockchain-rid", brid))
            }
            assertThat(res.message).isEqualTo("Option pubkey or container name needs to be specified.")
        }
    }

    @Test
    fun `Explicit setting system option using pubkey to get lease data`() {
        withModel(directoryChainModel, economyChainModel.withQuery(
                "get_leases_by_account", gtv(listOf(gtv(getLeaseData()), gtv(getLeaseData())))
        )) {
            val pubkey = "03DD65032C7BEE117FFDAA94C8DADAE79ADBA2A3A17C8D4A5239AA2DC2E93D845E"
            val brid = "0000000000000000000000000000000000000000000000000000000000000000"
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }

            command.parse(listOf("--api-url", "http://localhost:7745", "--blockchain-rid", brid, "--pubkey", pubkey))
            assertThat(logger.output()).contains(""""pubkey": "$pubkey"""")
        }
    }

    @Test
    fun `Explicit setting system option using container id to get lease data`() {
        withModel(directoryChainModel, economyChainModel.withQuery("get_lease_by_container_name", gtv(getLeaseData()))) {
            val brid = "0000000000000000000000000000000000000000000000000000000000000000"
            val command = DeployInspectLeaseCommand().context { terminal = testTerminal }

            command.parse(listOf("--api-url", "http://localhost:7745", "--blockchain-rid", brid, "--container-id", "container-id"))
            assertThat(logger.output()).contains(""""container": "container-id"""")
        }
    }

    @Test
    fun `Invalid public key with illegal characters`() {
        val pubkey = "000000000000000000000000000000000000000000000000000000000000000G"
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand().context { terminal = testTerminal }

        val res = assertThrows<Exception> {
            command.parse(listOf("--api-url", "http://localhost:7740", "--blockchain-rid", brid, "--pubkey", pubkey))
        }
        assertThat(res.message.toString()).contains("Public key contains one ore more illegal character. Supported Characters are: 0-9, A-F, a-f.")
    }

    @Test
    fun `Invalid public key of uneven length`() {
        val pubkey = "00000000000000000000000000000000000000000000000000000000000000034"
        val brid = "0000000000000000000000000000000000000000000000000000000000000000"
        val command = DeployInspectLeaseCommand().context { terminal = testTerminal }

        val res = assertThrows<Exception> {
            command.parse(listOf("--api-url", "http://localhost:7740", "--blockchain-rid", brid, "--pubkey", pubkey))
        }
        assertThat(res.message.toString()).contains("The public key must be a hex string with even length. Length was: ${pubkey.length}")
    }
}
