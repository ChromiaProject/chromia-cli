package com.chromia.cli

import assertk.assert
import assertk.assertions.contains
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.mock
import java.io.File
import java.nio.file.Path


class DeployCreateCommandTest {

    @Test
    fun cannotDeployFaultyConfig(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct module_args { name; } 
            """.trimIndent())
        }
        val settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  wrongConfig: 
                    module: main
                    moduleArgs:
                      main:
                        name: { nameIsInterprededAsDict }
                deployments:
                  test:
                    url: "localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent())
        }
        with(File(dir.toFile(), ".secret")) {
            writeText("""
                pubkey = 12312312414124124124121
                privkey = 000000000000000000000000000000000000001
            """.trimIndent())
        }
        val testConsole = TestConsole()
        val throwable = assertThrows<CliktError> {
            DeployCreateCommand(mock()).context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assert(throwable.message!!).contains("Module initialization failed: Decoding type 'text': expected STRING, actual DICT")
    }
}
