package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class CheckCommandTest {

    private val logger = TerminalRecorder(width = 1000, outputInteractive = true)
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        with(File(testDir.toFile(), "src/testDir/foo.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }

        settingsFile = File(testDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
                        test:
                            modules:
                                - testDir
                test:
                  modules:
                    - test
            """.trimIndent())
        }
    }

    @Test
    fun success() {
        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        CheckCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))
        assertThat(logger.output()).isEmpty()
    }

    @Test
    fun `production compile failure`() {
        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                quer hello() = "Hi!";
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        assertThrows<CliktError> {
            CheckCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))
        }
        assertThat(logger.output()).contains("ERROR: Syntax error")
    }

    @Test
    fun `test compile failure`() {
        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { ssert_equals(1, 2); }
            """.trimIndent())
        }

        assertThrows<CliktError> {
            CheckCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))
        }
        assertThat(logger.output()).contains("ERROR: Unknown name: 'ssert_equals'")
    }
}
