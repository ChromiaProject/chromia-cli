package com.chromia.cli.util

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class OptionalRemoteDeploymentTest {

    @TempDir
    private lateinit var testDir: Path

    @Test
    fun `should infer blockchain and network when configuration has single blockchain and single network`() {
        createChromiaYml("""
            blockchains:
              my_dapp:
                module: main
            deployments:
              testnet:
                url: https://node0.testnet.chromia.com:7740
                brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                chains:
                  my_dapp: x"1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef"
        """.trimIndent())

        val testCommand = TestCommand()

        val result = testCommand.test(listOf(
            "--settings", testDir.resolve("chromia.yml").toString(),
        ))

       val selected = testCommand.selectedDeploymentOption

       assertThat(result.statusCode).isEqualTo(0)
       assertThat(selected)
           .isInstanceOf(RemoteDeploymentOption::class)
           .transform { remote ->
               assertThat(remote.network).isEqualTo("testnet")
               assertThat(remote.blockchain).isEqualTo("my_dapp")
           }
    }

    @Test
    fun `should fallback to LocalDeploymentOption when no remote deployments configured`() {
        createChromiaYml("""
            blockchains:
              my_local_dapp:
                module: main
        """.trimIndent())

        val testCommand = TestCommand()

        val result = testCommand.test(listOf(
            "--settings", testDir.resolve("chromia.yml").toString(),
        ))
        
        val selected = testCommand.selectedDeploymentOption

        assertThat(result.statusCode).isEqualTo(0)

        assertThat(selected)
                .isInstanceOf(LocalDeploymentOption::class)
    }

    @Test
    fun `should fallback to LocalDeploymentOption when multiple networks exist without explicit selection`() {
        createChromiaYml("""
            blockchains:
                my_dapp:
                    module: main
            deployments:
              testnet:
                url: "$apiUrl"
                brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                chains:
                  my_dapp: x"1111111111111111111111111111111111111111111111111111111111111111"
              mainnet:
                url: "$apiUrl"
                brid: x"0000000000000000000000000000000000000000000000000000000000000002"
                chains:
                  my_dapp: x"2222222222222222222222222222222222222222222222222222222222222222"
        """.trimIndent())

        val testCommand = TestCommand()

        val result = testCommand.test(listOf(
            "--settings", testDir.resolve("chromia.yml").toString(),
        ))

        val selected = testCommand.selectedDeploymentOption

        assertThat(result.statusCode).isEqualTo(0)

        assertThat(selected)
                .isInstanceOf(LocalDeploymentOption::class)
    }

    @Test
    fun `should fallback to LocalDeploymentOption when multiple blockchains exist without explicit selection`() {
        createChromiaYml("""
            deployments:
              testnet:
                url: "$apiUrl"
                brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                chains:
                  main_dapp: x"1111111111111111111111111111111111111111111111111111111111111111"
                  secondary_dapp: x"2222222222222222222222222222222222222222222222222222222222222222"
                  third_dapp: x"3333333333333333333333333333333333333333333333333333333333333333"
        """.trimIndent())

        val testCommand = TestCommand()

        val result = testCommand.test(listOf(
            "--settings", testDir.resolve("chromia.yml").toString(),
        ))

        val selected = testCommand.selectedDeploymentOption

        assertThat(result.statusCode).isEqualTo(0)

        assertThat(selected)
                .isInstanceOf(LocalDeploymentOption::class)
    }

    @Test
    fun `should select RemoteDeploymentOption when explicit network and blockchain are provided`() {
        createChromiaYml("""
            deployments:
              testnet:
                url: "$apiUrl"
                brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                chains:
                  main_dapp: x"1111111111111111111111111111111111111111111111111111111111111111"
                  secondary_dapp: x"2222222222222222222222222222222222222222222222222222222222222222"
              mainnet:
                url: "$apiUrl"
                brid: x"0000000000000000000000000000000000000000000000000000000000000002"
                chains:
                  main_dapp: x"3333333333333333333333333333333333333333333333333333333333333333"
        """.trimIndent())

        val testCommand = TestCommand()

        val result = testCommand.test(listOf(
            "--settings", testDir.resolve("chromia.yml").toString(),
            "--network", "testnet",
            "--blockchain", "secondary_dapp"
        ))

        val selected = testCommand.selectedDeploymentOption

        assertThat(result.statusCode).isEqualTo(0)

        assertThat(selected)
                .isInstanceOf(RemoteDeploymentOption::class)
                .transform { remote ->
                    assertThat(remote.blockchain).isEqualTo("secondary_dapp")
                    assertThat(remote.network).isEqualTo("testnet")
                }
    }

    private fun createChromiaYml(content: String) {
        File(testDir.toFile(), "chromia.yml").writeText(content)
    }

    class TestCommand: CliktCommand() {
        lateinit var selectedDeploymentOption: DeploymentOption
        val settings by optionalChromiaModelConfigOption()
        val explicitTarget by LocalDeploymentOption({ settings.config })
        val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }

        override fun run() {
            try {
                selectedDeploymentOption = deploymentTarget or explicitTarget
            } catch (_: Exception) {}
        }
    }
}