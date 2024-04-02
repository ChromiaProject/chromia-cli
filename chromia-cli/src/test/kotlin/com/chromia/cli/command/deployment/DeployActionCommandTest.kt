package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.util.DeploymentTestDataCreator
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DeployActionCommandTest {

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
    fun cannotPauseNotDeployedBlockchain() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
        assertThat(res.output).contains("The action \"pause\" of Blockchain my_rell_dapp cannot be done since it has not been deployed to network test. Specify target blockchain rid in chromia.yml")
    }

    @Test
    fun parsePauseAttributeChainMissing() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing_chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing_chain] does not exist")
    }

    @Test
    fun parsePauseAttributeNetworkMissing() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "missing_network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing_network] does not exist")
    }

    @Test
    fun cannotResumeNotDeployedBlockchain() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
        assertThat(res.output).contains("The action \"resume\" of Blockchain my_rell_dapp cannot be done since it has not been deployed to network test. Specify target blockchain rid in chromia.yml")
    }

    @Test
    fun parseResumeAttributeChainMissing() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing_chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing_chain] does not exist")
    }

    @Test
    fun parseResumeAttributeNetworkMissing() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "missing_network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing_network] does not exist")
    }

    @Test
    fun cannotRemoveNotDeployedBlockchain() {
        val blockchain = "my_rell_dapp"
        val network = "test"
        val res = DeployRemoveCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", blockchain, "--network", network))
        assertThat(res.stdout).contains("The action \"remove\" of Blockchain $blockchain cannot be done since it has not been deployed to network $network. Specify target blockchain rid in chromia.yml")
    }
}
