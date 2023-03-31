package com.chromia.cli

import assertk.assert
import assertk.assertions.contains
import com.chromia.cli.util.TestClient
import com.chromia.cli.util.TestClusterManagement
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class DeployUpdateCommandTest {

    companion object {
        lateinit var settings: File
        lateinit var secret: File

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir dir: Path) {
            println("hello")
            with(File(dir.toFile(), "src/main.rell")) {
                parentFile.mkdirs()
                writeText("""
                module;
                struct module_args { name; } 
            """.trimIndent())
            }
            with(File(dir.toFile(), "src/mainNoArgs.rell")) {
                parentFile.mkdirs()
                writeText("""
                module;
            """.trimIndent())
            }
            settings = File(dir.toFile(), "config.yml").apply {
                writeText("""
                blockchains:
                  wrongConfig: 
                    module: main
                    moduleArgs:
                      main:
                        name: { nameIsInterprededAsDict }
                  okConfig:
                    module: mainNoArgs
                  notDeployed:
                    module: mainNoArgs
                deployments:
                  test:
                    url: "localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    container: foo
                    chains:
                      okConfig: x"0000000000000000000000000000000000000000000000000000000000000002"
            """.trimIndent())
            }
            secret = File(dir.toFile(), ".secret").apply {
                writeText("""
privkey = BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114
pubkey = 03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05
            """.trimIndent())
            }

        }
    }

    lateinit var testConsole: TestConsole

    @BeforeEach
    fun setupTest() {
        testConsole = TestConsole()
    }

    @Test
    fun successfulDeployment() {
        DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "okConfig", "--network", "test", "--secret", secret.absolutePath))
        testConsole.assertContains("Deployment of blockchain okConfig was successful\n")
    }

    @Test
    fun cannotDeployFaultyConfig(@TempDir dir: Path) {
        val throwable = assertThrows<CliktError> {
            DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assert(throwable.message!!).contains("Module initialization failed: Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun deploymentMustExistToUpdate() {
        val throwable = assertThrows<PrintMessage> {
            DeployUpdateCommand({ TestClient(it) }, { TestClusterManagement() }).context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "notDeployed", "--network", "test", "--secret", secret.absolutePath))
        }
        assert(throwable.message!!).contains("Blockchain notDeployed cannot be updated since it has not been deployed to network test")
    }
}
