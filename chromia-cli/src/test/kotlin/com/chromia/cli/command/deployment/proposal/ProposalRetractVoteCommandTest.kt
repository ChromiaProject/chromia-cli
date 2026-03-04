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
import com.chromia.cli.util.ClusterManagementStub
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.parse
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

internal class ProposalRetractVoteCommandTest {

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
    fun canNotFindIdentityToSignRetractVoteActionWithTest() {
        assertThrows<CanNotFindPubkeyException> {
            withModel(model.withClusterManagement(ClusterManagementStub())) {
                ProposalRetractVoteCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
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
                withModel(model.withClusterManagement(ClusterManagementStub()).withStatus(TransactionStatus.REJECTED, "message")) {
                    ProposalRetractVoteCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
                }
            }
            assertThat(response.message).isEqualTo("Proposal action failed with reason message")
        }
    }

    @Test
    fun retractVotePassTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            withModel(model.withClusterManagement(ClusterManagementStub())) {
                val res = ProposalRetractVoteCommand().test(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
                assertThat(res.stdout).contains("Vote on proposal 1 successfully retracted")
            }
        }
    }
}
