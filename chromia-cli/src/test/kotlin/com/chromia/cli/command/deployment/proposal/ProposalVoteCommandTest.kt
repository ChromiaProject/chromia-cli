package com.chromia.cli.command.deployment.proposal

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.withClusterManagement
import com.chromia.build.tools.restapi.withStatus
import com.chromia.build.tools.testData
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.TestClusterManagement
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.testing.test
import net.postchain.common.tx.TransactionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test

internal class ProposalVoteCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    val model = DirectoryChainModel()


    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @Test
    fun canNotFindIdentityToSignProposalVoteActionWithTest() {
        assertThrows<CanNotFindPubkeyException> {
            withModel(model.withClusterManagement(TestClusterManagement())) {
                ProposalVoteCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1", "--accept"))
            }
        }
    }

    @Test
    fun transactionRejectionTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            val response = assertThrows<PrintMessage> {
                withModel(model.withClusterManagement(TestClusterManagement()).withStatus(TransactionStatus.REJECTED, "message")) {
                    ProposalVoteCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1", "--accept"))
                }
            }

            assertThat(response.message).isEqualTo("Proposal action failed with reason message")
        }
    }

    @Test
    fun transactionRevokePassTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {

            withModel(model.withClusterManagement(TestClusterManagement())) {
                val res = ProposalVoteCommand().test(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1", "--accept"))
                assertThat(res.stdout).contains("Successfully voted on proposal 1")
            }
        }
    }
}