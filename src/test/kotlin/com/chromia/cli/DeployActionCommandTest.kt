package com.chromia.cli

import assertk.assert
import assertk.assertions.contains
import com.chromia.cli.util.TestClient
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class DeployActionCommandTest {

    companion object {
        lateinit var settings: File
        lateinit var secret: File

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir dir: Path) {
            with(File(dir.toFile(), "src/mainNoArgs.rell")) {
                parentFile.mkdirs()
                writeText("""
                module;
            """.trimIndent())
            }
            settings = File(dir.toFile(), "config.yml").apply {
                writeText("""
                blockchains:
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
    fun cannotPauseNotDeployedBlockchain(@TempDir dir: Path) {
        val throwable = assertThrows<CliktError> {
            DeployPauseCommand { TestClient(it) }.context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "notDeployed", "--network", "test"))
        }
        assert(throwable.message!!).contains("The action \"pause\" of Blockchain notDeployed cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun cannotResumeNotDeployedBlockchain(@TempDir dir: Path) {
        val throwable = assertThrows<CliktError> {
            DeployResumeCommand { TestClient(it) }.context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "notDeployed", "--network", "test"))
        }
        assert(throwable.message!!).contains("The action \"resume\" of Blockchain notDeployed cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }

    @Test
    fun cannotRemoveNotDeployedBlockchain(@TempDir dir: Path) {
        val throwable = assertThrows<CliktError> {
            DeployRemoveCommand { TestClient(it) }.context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "notDeployed", "--network", "test"))
        }
        assert(throwable.message!!).contains("The action \"remove\" of Blockchain notDeployed cannot be done since it has not been deployed to network test. Specify target blockchain rid in config.yml")
    }
}
