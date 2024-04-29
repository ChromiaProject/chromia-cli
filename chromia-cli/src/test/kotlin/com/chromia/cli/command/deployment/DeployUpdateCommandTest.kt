package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.withClusterManagement
import com.chromia.build.tools.restapi.withCompression
import com.chromia.build.tools.restapi.withInvalidConfiguration
import com.chromia.build.tools.restapi.withRellVersion
import com.chromia.build.tools.restapi.withValidConfiguration
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.TestClusterManagement
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.test.assertNotNull
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.api.base.RellCliBasicException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir


class DeployUpdateCommandTest {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var secret: File

    val model = DirectoryChainModel().withClusterManagement(TestClusterManagement())

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
    }


    @Test
    fun successfulDeployment() {
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withValidConfiguration()) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
            assertThat(model.txQueue.size).isEqualTo(1)
            val operations = model.txQueue.first().gtxBody.operations
            assertThat(operations.map { it.opName }).containsExactly("nop", "propose_configuration")
            assertThat(operations.last().args).containsAll(gtv("03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05".hexStringToByteArray()), gtv("0000000000000000000000000000000000000000000000000000000000000002".hexStringToByteArray()), gtv(""))
            assertThat(res.output).contains("Blockchain deployed was successfully updated on network test")
            val deployedFile = testDir.resolve("build").listDirectoryEntries("test_deployed_*").first()
            assertThat(deployedFile.toFile().readText()).contains("<entry key=\"compressed_roots\">")
        }
    }

    @Test // TODO: Should only work if chain is in system cluster
    fun successfulDeploymentOnHeight() {
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withValidConfiguration()) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--height", "100"))
            assertThat(res.output).contains("Blockchain deployed was successfully updated on network test")
        }
    }

    @Test
    fun failDeploymentOnHeightWithMultipleChains() {
        withModel(model) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed,hello", "--network", "test", "--height", "100"))
            assertThat(res.stderr).contains("Error: invalid value for --height: When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify")
        }
    }

    @Test
    fun cannotDeployFaultyConfig() {

        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct module_args { name; } 
            """.trimIndent())
        }

        val throwable = assertThrows<RellCliBasicException> {
            DeployUpdateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun deploymentMustExistToUpdate() {
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withValidConfiguration()) {
            val throwable = assertThrows<PrintMessage> {
                DeployUpdateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("Blockchain my_rell_dapp cannot be updated since it has not been deployed to network test")
        }
    }

    @Test
    fun cannotUpdateInvalidConfigChange() {
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withInvalidConfiguration()) {
            val throwable = assertThrows<PrintMessage> {
                DeployUpdateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("Blockchain deployed cannot be updated on network test. Reason: Invalid configuration")
        }
    }

    @Test
    fun verifyOnly() {
        // TODO: Remove withCompression when update command is refactored
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withValidConfiguration()) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--verify-only"))
            assertThat(res.output).contains("Blockchain deployed was successfully verified against deployed chain on network test")
            assertThat(res.output).doesNotContain("Blockchain deployed was successfully updated on network test")
        }
    }

    @Test
    fun skipVerification() {
        withModel(model.withCompression(), DeploymentTestDataCreator.deployedChainModel.withInvalidConfiguration()) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--skip-verification"))
            assertThat(res.output).contains("Skipping verification of blockchain config")
            assertThat(res.output).doesNotContain("Blockchain deployed was successfully verified against deployed chain on network test")
            assertThat(res.output).contains("Blockchain deployed was successfully updated on network test")
        }
    }

    @Test
    fun successfulDeploymentWithoutCompression() {
        withModel(model, DeploymentTestDataCreator.deployedChainModel.withValidConfiguration()) {
            val res = DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--no-compression"))
            assertThat(res.output).contains("Blockchain deployed was successfully updated on network test")
            val buildGtx = testDir.resolve("build").listDirectoryEntries().find { it.name.startsWith("test_deployed") }?.toFile()?.readText()
            assertNotNull(buildGtx)
            assertThat(buildGtx).doesNotContain("<entry key=\"compressed_roots\">")
        }
    }

    @Test
    fun cannotUpdateNotMatchingRellVersion() {
        withModel(model.withRellVersion("0.11.0")) {
            val throwable = assertThrows<RellDeployVersionException> {
                DeployUpdateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("The local compile version 0.13.5 is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                    "The deployment is aborted.")
        }
    }
}
