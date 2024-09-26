package com.chromia.cli.command.deployment.proposal

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isInstanceOf
import com.chromia.build.tools.restapi.*
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import com.chromia.cli.it.AlwaysFailingModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.testing.test
import net.postchain.common.tx.TransactionStatus
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class ProposalRenameBlockchainCommandTest {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var secret: File
    private lateinit var config: File

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        config = testDir.resolve("config").toFile()
    }

    @Test
    fun cannotProposeRenameBlockchainIfApiVersionIsNotGreaterThanSixtyOneTest() {
        withModel(DirectoryChainModel(apiVersion = 60)) {
            testData(testDir) {
                config {
                    deployments("""
                        deployments:
                            test:
                                url: "http://localhost:7745"
                                brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    """.trimIndent())
                }
            }
            val throwable = assertThrows<CliktError> {
                ProposalRenameBlockchainCommand().parse(listOf(
                        "-s", settingsFile.absolutePath,
                        "--network", "test",
                        "--config", config.absolutePath,
                        "-n", "testname",
                        "--description", "Proposed rename",
                        "--secret", secret.absolutePath
                ))
            }
            assertThat(
                throwable.message!!
            ).contains(
                "Blockchain rename operation requires directory chain version 61, found version 60"
            )
        }
    }

    @Test
    fun proposeRenameBlockchainWithoutPubKeyTest() {
        withModel(DirectoryChainModel(apiVersion = 65)) {
            testData(testDir) {
                config {
                    deployments("""
                        deployments:
                            test:
                                url: "http://localhost:7745"
                                brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    """.trimIndent())
                }
            }
            val throwable = assertThrows<CanNotFindPubkeyException> {
                ProposalRenameBlockchainCommand().parse(listOf(
                        "-s", settingsFile.absolutePath,
                        "--network", "test",
                        "--config", config.absolutePath,
                        "-n", "New blockchain name",
                        "--description", "Proposed name"
                ))
            }

            assertThat(throwable).isInstanceOf(CanNotFindPubkeyException::class.java)
            assertThat(throwable.message!!).contains("Could not find a pubkey to act on proposals with")
        }
    }

    @Test
    fun proposeRenameBlockchainTest() {
        withModel(DirectoryChainModel(apiVersion = 65)) {
            testData(testDir) {
                config {
                    deployments("""
                        deployments:
                            test:
                                url: "http://localhost:7745"
                                brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    """.trimIndent())
                }
            }
            val res = ProposalRenameBlockchainCommand().test(listOf(
                    "-s", settingsFile.absolutePath,
                    "--network", "test",
                    "--config", config.absolutePath,
                    "-n", "New blockchain name",
                    "--description", "Proposed name",
                    "--secret", secret.absolutePath
            ))
            assertThat(res.stdout).contains("Blockchain rename proposition was added successfully")
        }
    }

    @Test
    fun proposeRenameBlockchainTransactionRejectedTest() {
        withModel(AlwaysFailingModel(DirectoryChainModel(apiVersion = 65), TransactionStatus.REJECTED)) {
            testData(testDir) {
                config {
                    deployments("""
                        deployments:
                            test:
                                url: "http://localhost:7745"
                                brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    """.trimIndent())
                }
            }
            val res = ProposalRenameBlockchainCommand().test(listOf(
                    "-s", settingsFile.absolutePath,
                    "--network", "test",
                    "--config", config.absolutePath,
                    "-d", "test",
                    "-n", "New blockchain name",
                    "--description", "Proposed name",
                    "--secret", secret.absolutePath
            ))
            assertThat(res.stdout).contains("Cannot add proposal for renaming blockchain reason null")
        }
    }
}