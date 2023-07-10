package com.chromia.cli.unit

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.DeployUpdateCommand
import com.chromia.cli.it.TestDataCreator
import com.chromia.cli.util.TestClient
import com.chromia.cli.util.TestClusterManagement
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.testing.test
import net.postchain.rell.api.base.RellCliBasicException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class DeployUpdateCommandTest {

    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var secret: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        testDir = dir
        settingsFile = testDir.resolve("config.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        TestDataCreator.basicApp(dir)

    }


    @Test
    fun successfulDeployment() {
        val res = DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
        assertThat(res.output).contains("Deployment of blockchain deployed was successful")
    }

    @Test
    fun successfulDeploymentOnHeight() {
        val res = DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test", "--height", "100"))
        assertThat(res.output).contains("Deployment of blockchain deployed was successful")
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
