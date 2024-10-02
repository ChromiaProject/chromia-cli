package com.chromia.cli.command.deployment.voterset

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.withStatus
import com.chromia.build.tools.testData
import com.chromia.cli.util.DeploymentTestDataCreator
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.tx.TransactionStatus
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test

internal class VotersetUpdateCommandTest {
    val model = DirectoryChainModel()

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @AfterEach
    fun tearDown() {
        logger.clearOutput()
    }

    @Test
    fun updateTransactionRejectionTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            val response = assertThrows<PrintMessage> {
                RestApiInstance.withModel(model.withStatus(TransactionStatus.REJECTED, "REASON")) {
                    VotersetUpdateCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "-vs", "vs1"))
                }
            }
            assertThat(response.message).isEqualTo("Failed to add proposal with reason REASON")
        }
    }

    @Test
    fun updateTransactionSuccessTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            RestApiInstance.withModel(model) {
                val response = VotersetUpdateCommand().test(listOf("--settings", settingsFile.absolutePath, "--network", "test", "-vs", "vs1"))
                assertThat(response.output).contains("Proposal for voter set vs1 has been added")
            }
        }
    }

    @Test
    fun updateTransactionFailCanNotConvertAddMemberPubkeyTest() {
        val response = assertThrows<BadParameterValue> {
            RestApiInstance.withModel(model) {
                VotersetUpdateCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "-vs", "vs1", "--add-member", "NOTHEXSTRING"))
            }
        }
        assertThat(response.message).isEqualTo("Char N is not a hex digit")
    }

    @Test
    fun updateTransactionFailCanNotConvertRemoveMemberPubkeyTest() {
        val response = assertThrows<BadParameterValue> {
            RestApiInstance.withModel(model) {
                VotersetUpdateCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "-vs", "vs1", "--remove-member", "NOTHEXSTRING"))
            }
        }
        assertThat(response.message).isEqualTo("Char N is not a hex digit")
    }

    @Test
    fun updateTransactionFailToLongDescriptionTest() {
        val response = assertThrows<BadParameterValue> {
            RestApiInstance.withModel(model) {
                VotersetUpdateCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "-vs", "vs1", "--description", """
                    Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore 
                    et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut 
                    aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum 
                    dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui 
                    officia deserunt mollit anim id est laborum.

                    Curabitur pretium tincidunt lacus. Nulla gravida orci a odio. Nullam varius, turpis et commodo pharetra,
                    est eros bibendum elit, nec luctus magna felis sollicitudin mauris. Integer in mauris eu nibh euismod
                    gravida. Duis ac tellus et risus vulputate vehicula. Donec lobortis risus a elit. Etiam tempor. 
                    Ut ullamcorper, ligula eu tempor congue, eros est euismod turpis, id tincidunt sapien risus a quam.
                    Maecenas fermentum consequat mi. Pellentesque malesuada nulla a mi. Aliquam faucibus, elit ut dictum aliquet, 
                    felis nisl adipiscing sapien, sed malesuada diam lacus eget erat.
                """.trimIndent()))
            }
        }
        assertThat(response.message).isEqualTo("value is too long, maximum allowed length is 1000, current length is 1031.")
    }
}