package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.chromia.build.tools.config.getProviderUrlsForNetwork
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DeploymentOptionTest {

    @TempDir
    private lateinit var testDir: Path

    @Test
    fun `test no blockchains configured error`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url: "http://localhost:7740"
            """.trimIndent()
        )

        val res = assertThrows<CliktError> {
            TestCommandWithRemoteDeployment(getValues = true)
                    .parse(listOf("--settings", settingsFile.absolutePath, "--network", "test-network"))
        }
        assertThat(res.message!!).contains("No blockchains configured for deployment 'test-network'")
        assertThat(res.message!!).contains("Resolution options:")
        assertThat(res.message!!).contains("Add blockchain configurations to your deployment")
        assertThat(res.message!!).contains("Choose a different deployment target using --network")
    }

    @Test
    fun `test multiple blockchains available error`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
                  blockchain2: x"0000000000000000000000000000000000000000000000000000000000000002"
            """.trimIndent()
        )

        val res = assertThrows<CliktError> {
            TestCommandWithRemoteDeployment(getValues = true)
                    .parse(listOf("--settings", settingsFile.absolutePath, "--network", "test-network"))
        }

        assertThat(res.message!!).contains("Multiple blockchains available in deployment 'test-network'")
        assertThat(res.message!!).contains("Available chains: blockchain1, blockchain2")
        assertThat(res.message!!).contains("Resolution:")
        assertThat(res.message!!).contains("Specify the target blockchain using: --blockchain <name>")
        assertThat(res.message!!).contains("Example: --blockchain blockchain1")
    }

    @Test
    fun `test blockchain not found in deployment error`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val res = assertThrows<IllegalArgumentException> {
            TestCommandWithRemoteDeployment(getValues = true)
                    .parse(listOf(
                            "--settings",
                            settingsFile.absolutePath,
                            "--network",
                            "test-network",
                            "--blockchain",
                            "nonexistent-blockchain"
                    ))
        }

        assertThat(res.message!!).isEqualTo("Blockchain named nonexistent-blockchain not found in deployment configuration")
    }

    @Test
    fun `test successful single blockchain deployment without explicit blockchain`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val command = TestCommandWithRemoteDeployment()
        val result = command.test(listOf("--settings", settingsFile.absolutePath, "--network", "test-network"))

        assertThat(result.stderr).doesNotContain("No blockchains configured")
        assertThat(result.stderr).doesNotContain("Multiple blockchains available")
        assertThat(result.stderr).doesNotContain("not found in configuration")
    }

    @Test
    fun `test uses url from deployment model if available for chromia networks for remote option`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
                """
            deployments:
              testnet:
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val command = TestCommandWithRemoteDeployment()
        command.test(listOf("--settings", settingsFile.absolutePath, "--network", "testnet"))
    }

    @Test
    fun `test correct loading of mainnet urls from hardcoded list`() {
        var command = TestCommandWithRemoteDeployment(getProviderUrlsForNetwork("mainnet")!!)
        command.test(listOf("--mainnet"))

        command = TestCommandWithRemoteDeployment(getProviderUrlsForNetwork("testnet")!!)
        command.test(listOf("--testnet"))

//        command = TestCommandWithRemoteDeployment(getProviderUrlsForNetwork("devnet1")!!)
//        command.test(listOf("--devnet1"))

        command = TestCommandWithRemoteDeployment(getProviderUrlsForNetwork("devnet2")!!)
        command.test(listOf("--devnet2"))
    }

    @Test
    fun `uses url from deployment model for chromia networks for deployed option`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
                """
            deployments:
              testnet:
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val command = TestCommandWithDeployedNetworkOption()
        command.test(listOf("--settings", settingsFile.absolutePath, "--network", "testnet"))
    }

    @Test
    fun `loads urls from predefined networks when url is not in chromia model for deployed option`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
                """
            deployments:
              testnet:
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val command = TestCommandWithDeployedNetworkOption(getProviderUrlsForNetwork("testnet")!!)
        command.test(listOf("--settings", settingsFile.absolutePath, "--network", "testnet"))
    }

    @Test
    fun `throws when no urls is defined for non default network name`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText(
                """
            deployments:
              non_default_network:
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent()
        )

        val command = TestCommandWithDeployedNetworkOption()
        val res = assertThrows<IllegalArgumentException> {
            command.test(listOf("--settings", settingsFile.absolutePath, "--network", "non_default_network"))
        }
        assertThat(res.message!!).isEqualTo("No urls found for network non_default_network")
    }
}

private class TestCommandWithRemoteDeployment(
        val expectedUrls: List<String> = listOf("http://localhost:7740"),
        val getValues: Boolean = false
) : CliktCommand() {
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        if (getValues) {
            target.blockchain
            target.brid
            target.urls
        }
        assertOnTargetUrl(target)
    }

    fun assertOnTargetUrl(target: DeploymentOption) {
        assertThat(target.urls).isEqualTo(expectedUrls)
    }
}

private class TestCommandWithDeployedNetworkOption(
        val expectedUrls: List<String> = listOf("http://localhost:7740"),
) : CliktCommand() {
    private val settings by chromiaModelConfigOption()
    val networkTarget by DeployedNetworkOption { settings.model }

    override fun run() {
        val target = networkTarget
        assertOnTargetUrl(target)
    }

    fun assertOnTargetUrl(target: DeploymentOption) {
        assertThat(target.urls).isEqualTo(expectedUrls)
    }
}
