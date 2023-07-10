package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.it.TestDataCreator
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
        TestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("config.yml").toFile()
        secret = testDir.resolve(".secret").toFile()

    }

    @Test
    fun cannotPauseNotDeployedBlockchain() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        assertThat(res.output).contains("The action \"pause\" of Blockchain hello cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun parsePauseAttributeChainMissing() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parsePauseAttributeNetworkMissing() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }

    @Test
    fun cannotResumeNotDeployedBlockchain() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        assertThat(res.output).contains("The action \"resume\" of Blockchain hello cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun parseResumeAttributeChainMissing() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parseResumeAttributeNetworkMissing() {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }
}
