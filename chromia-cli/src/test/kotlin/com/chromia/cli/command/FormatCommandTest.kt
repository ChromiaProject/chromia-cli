package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText

internal class FormatCommandTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    private val mainRellContent = """
            module;
            query hello() {
            return "Hi!";
            }  
        """.trimIndent()
    private val testRellContent = """
        @test module;

        function test_a() {}

        function test_b() {}
        
    """.trimIndent()
    private val formattedMainRellContent = """
        module;
        
        query hello() {
          return "Hi!";
        }

    """.trimIndent()
    private val myLibContent = """
            module;

            function my_lib_hello() {
                var x = 5;
            }

        """.trimIndent()
    private val otherLibContent = """
            module;

            function other_lib_hello() {
                var x = 5;
            }

        """.trimIndent()

    @BeforeEach
    fun setup() {
        with(File(testDir.toFile(), "src/main/module.rell")) {
            parentFile.mkdirs()
            writeText(mainRellContent)
        }

        with(File(testDir.toFile(), "src/test/test.rell")) {
            parentFile.mkdirs()
            writeText(testRellContent)
        }

        with(File(testDir.toFile(), "src/lib/my_lib/module.rell")) {
            parentFile.mkdirs()
            writeText(myLibContent)
        }

        with(File(testDir.toFile(), "src/lib/other_lib/module.rell")) {
            parentFile.mkdirs()
            writeText(otherLibContent)
        }

        with(File(testDir.toFile(), ".rell_format")) {
            writeText("""
                [*.rell]
                tab_size = 2
                
            """.trimIndent())
        }

        with(File(testDir.toFile(), ".my_rell_format")) {
            writeText("""
                [*.rell]
                tab_size = 3
                
            """.trimIndent())
        }

        settingsFile = File(testDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
                libs:
                    other_lib:
                        registry: https://bitbucket.org/chromawallet/ft3-lib
                        path: rell/src/lib/ft4
                        tagOrBranch: v0.7.0r
                        rid: x"F7C207AA595ABD25FDE5C2C2E32ECD3768B480AD03D1F2341548FF4F37D9B7AF"
                        insecure: false
            """.trimIndent())
        }
    }

    @Test
    fun testFormatDefault() {
        FormatCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))

        assertThat(logger.output()).contains("Formatting test/test.rell... no changes")
        assertThat(logger.output()).contains("Formatting lib/my_lib/module.rell... changed")
        assertThat(logger.output()).contains("Formatting main/module.rell... changed")
        assertThat(logger.output()).doesNotContain("lib/other_lib/module.rell")
        assertThat(testDir.resolve("src/main/module.rell").readText()).isEqualTo(formattedMainRellContent)
        assertThat(testDir.resolve("src/test/test.rell").readText()).isEqualTo(testRellContent)
    }

    @Test
    fun testFormatSourceDir() {
        FormatCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--source-dir", testDir.resolve("src/main").toString()))
        assertThat(logger.output()).isEqualTo("""
            Formatting module.rell... changed
            
        """.trimIndent())

        assertThat(testDir.resolve("src/main/module.rell").readText()).isEqualTo(formattedMainRellContent)
    }

    @Test
    fun testFormatSingleFile() {
        val singleFile = testDir.resolve("src/main/module.rell").toString()
        FormatCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--file", singleFile))
        assertThat(logger.output()).isEqualTo("""
            Formatting ${singleFile}... changed
            
        """.trimIndent())

        assertThat(testDir.resolve("src/main/module.rell").readText()).isEqualTo(formattedMainRellContent)
    }

    @Test
    fun testFormatOptions() {
        FormatCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--formatter-options", testDir.resolve(".my_rell_format").toString()))
        assertThat(logger.output()).contains("Formatting test/test.rell... no changes")
        assertThat(logger.output()).contains("Formatting lib/my_lib/module.rell... changed")
        assertThat(logger.output()).contains("Formatting main/module.rell... changed")
        assertThat(logger.output()).doesNotContain("lib/other_lib/module.rell")
        assertThat(testDir.resolve("src/main/module.rell").readText()).isEqualTo("""
                module;
                
                query hello() {
                   return "Hi!";
                }
        
            """.trimIndent())
    }
}
