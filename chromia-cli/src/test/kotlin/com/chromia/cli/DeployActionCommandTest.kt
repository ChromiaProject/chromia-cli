package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.DeployPauseCommand
import com.chromia.cli.DeployResumeCommand
import com.chromia.cli.it.TestDataCreator
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class DeployActionCommandTest {

    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var secret: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        TestDataCreator.unitTestApp(dir)
        testDir = dir
        settingsFile = testDir.resolve("config.yml").toFile()
        secret = testDir.resolve(".secret").toFile()

    }

    @Test
    fun cannotPauseNotDeployedBlockchain() {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        assertThat(res.output).contains("The action \"pause\" of Blockchain hello cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun parsePauseAttributeChainMissing(@TempDir dir: Path) {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parsePauseAttributeNetworkMissing(@TempDir dir: Path) {
        val res = DeployPauseCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }

    @Test
    fun cannotResumeNotDeployedBlockchain(@TempDir dir: Path) {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        assertThat(res.output).contains("The action \"resume\" of Blockchain hello cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun parseResumeAttributeChainMissing(@TempDir dir: Path) {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parseResumeAttributeNetworkMissing(@TempDir dir: Path) {
        val res = DeployResumeCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }
}
