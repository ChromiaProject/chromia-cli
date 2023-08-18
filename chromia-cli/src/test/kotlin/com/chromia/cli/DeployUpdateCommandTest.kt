package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.TestClient
import com.chromia.cli.util.TestClusterManagement
import com.chromia.cli.util.TestConfiguration
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
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

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
    }


    @Test
    fun successfulDeployment() {
        val config = TestConfiguration()
        val res = DeployUpdateCommand({ TestClient(it, config) }, { TestClusterManagement() }).test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
        assertThat(config.txs.size).isEqualTo(1)
        val operations = config.txs.first().gtxBody.operations
        assertThat(operations.map { it.opName }).containsExactly("nop", "propose_configuration")
        assertThat(operations.last().args).containsAll(gtv("03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05".hexStringToByteArray()), gtv("0000000000000000000000000000000000000000000000000000000000000002".hexStringToByteArray()), gtv(""))
        assertThat(res.output).contains("Update of blockchain deployed was successful")
    }

    @Test // TODO: Should only work if chain is in system cluster
    fun successfulDeploymentOnHeight() {
        val config = TestConfiguration()
        val res = DeployUpdateCommand({ TestClient(it, config) }, { TestClusterManagement() }).test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--height", "100"))
        assertThat(res.output).contains("Update of blockchain deployed was successful")
    }

    @Test
    fun failDeploymentOnHeightWithMultipleChains() {
        val res = DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed,hello", "--network", "test", "--height", "100"))
        assertThat(res.stderr).contains("Error: invalid value for --height: When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify")
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
            DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun deploymentMustExistToUpdate() {
        val throwable = assertThrows<PrintMessage> {
            DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Blockchain hello cannot be updated since it has not been deployed to network test")
    }
}
