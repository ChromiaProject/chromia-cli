package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ContainerConfigurationCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var secret: File

    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
    }

    @Test
    fun `successful container configuration proposal with slow DB statement log ms`() {
        settingsFile.appendText(
            """

            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    container: test_container
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val directoryChainModel = DirectoryChainModel()
        val economyChainModel = TestModel(BlockchainRid.buildRepeat(1), 1)

        withModel(directoryChainModel, economyChainModel) {
            val command = ContainerConfigurationCommand().context { terminal = testTerminal }
            command.parse(
                listOf(
                    "--network",
                    "test_network",
                    "-s",
                    settingsFile.absolutePath,
                    "--secret",
                    secret.absolutePath,
                    "--slow-db-statement-log-ms",
                    "1000"
                )
            )
            assertThat(logger.output()).contains("Container configuration proposal submitted")
            assertThat(logger.output()).contains("slow-db-statement-log-ms: 1000")
        }
    }

    @Test
    fun `successful container configuration proposal with custom description`() {
        settingsFile.appendText(
            """

            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    container: test_container
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val directoryChainModel = DirectoryChainModel()
        val economyChainModel = TestModel(BlockchainRid.buildRepeat(1), 1)

        withModel(directoryChainModel, economyChainModel) {
            val command = ContainerConfigurationCommand().context { terminal = testTerminal }
            command.parse(
                listOf(
                    "--network", "test_network",
                    "-s", settingsFile.absolutePath,
                    "--secret", secret.absolutePath,
                    "--slow-db-statement-log-ms", "2000",
                    "--description", "Custom configuration update"
                )
            )
            assertThat(logger.output()).contains("Container configuration proposal submitted")
            assertThat(logger.output()).contains("Custom configuration update")
        }
    }

    @Test
    fun `fails when no container is specified in deployment config`() {
        settingsFile.appendText(
            """

            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val directoryChainModel = DirectoryChainModel()
        val economyChainModel = TestModel(BlockchainRid.buildRepeat(1), 1)

        withModel(directoryChainModel, economyChainModel) {
            val throwable = assertThrows<Exception> {
                ContainerConfigurationCommand().parse(
                    listOf(
                        "--network",
                        "test_network",
                        "-s",
                        settingsFile.absolutePath,
                        "--secret",
                        secret.absolutePath,
                        "--slow-db-statement-log-ms",
                        "1000"
                    )
                )
            }
            assertThat(throwable.message)
                .isEqualTo("No container specified, please update your chromia.yml file accordingly")
        }
    }

    @Test
    fun `fails when no signers are configured`() {
        val settingsWithoutSecret = testDir.resolve("chromia_no_secret.yml").toFile()
        settingsWithoutSecret.writeText(
            """
            deployments:
                test_network:
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    url:
                      - ${RestApiInstance.apiUrl}
                    container: test_container
                    chains:
                      blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val directoryChainModel = DirectoryChainModel()
        val economyChainModel = TestModel(BlockchainRid.buildRepeat(1), 1)

        withModel(directoryChainModel, economyChainModel) {
            val throwable = assertThrows<Exception> {
                ContainerConfigurationCommand().parse(
                    listOf(
                        "--network",
                        "test_network",
                        "-s",
                        settingsWithoutSecret.absolutePath,
                        "--slow-db-statement-log-ms",
                        "1000"
                    )
                )
            }
            assertThat(throwable.message).isEqualTo("No signers configured")
        }
    }
}
