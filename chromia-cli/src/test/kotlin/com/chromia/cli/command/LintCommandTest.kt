package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.rell.api.base.RellCliExitException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.createDirectory
import kotlin.io.path.readText

internal class LintCommandTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    private val mainRellContent = """
            module;
            query Hello() {
            var x = 5;
                val y = 10;
                var Sum = x + y;
                return "Hi!";
            }  
        """.trimIndent()
    private val testRellContent = """
        @test module;

        function test_a() {}

        function test_b() {}
        
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
                tab_size = 4

            """.trimIndent())
        }

        with(File(testDir.toFile(), ".rell_lint")) {
            writeText("""
                [*.rell]
                rule_naming_convention=true
                rule_import_from_non_module=true
                rule_quote_format=double
                rule_formatter=true
                rule_constant_detection=true
                rule_unused_variable=true
            """.trimIndent())
        }

        with(File(testDir.toFile(), ".my_rell_format")) {
            writeText("""
                [*.rell]
                tab_size = 2
                
            """.trimIndent())
        }

        with(File(testDir.toFile(), ".my_rell_lint")) {
            writeText("""
                [*.rell]
                rule_quote_format=single
                rule_formatter=false
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
    fun testLintDefault() {
        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))
        }
        assertThat(logger.output()).contains("""
        main/module.rell
            linter_issue:rule_constant_detection - Variable 'x' is never modified, so it can be declared using 'val'
            linter_issue:rule_constant_detection - Variable 'Sum' is never modified, so it can be declared using 'val'
            linter_issue:rule_naming_convention - 'Hello' should be in snake case
            linter_issue:rule_unused_variable - Variable 'Sum' is never used
            linter_issue:rule_naming_convention - 'Sum' should be in snake case
            linter_issue:formatting - Insert: `⏎` at line 2, column 1
            linter_issue:formatting - Insert: `····` at line 3, column 1
            linter_issue:formatting - Change: `··` to '⏎' at line 7, column 2
        """.trimIndent())

        assertThat(logger.output()).doesNotContain("test/test.rell")

        assertThat(logger.output()).contains("""
            lib/my_lib/module.rell
                linter_issue:rule_constant_detection - Variable 'x' is never modified, so it can be declared using 'val'
                linter_issue:rule_unused_variable - Variable 'x' is never used
        """.trimIndent())

        assertThat(logger.output()).doesNotContain("lib/other_lib/module.rell")
    }

    @Test
    fun testLintSourceDir() {
        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--source-dir", testDir.resolve("src/main").toString()))
        }
        assertThat(logger.output()).contains("""
        module.rell
            linter_issue:rule_constant_detection - Variable 'x' is never modified, so it can be declared using 'val'
            linter_issue:rule_constant_detection - Variable 'Sum' is never modified, so it can be declared using 'val'
            linter_issue:rule_naming_convention - 'Hello' should be in snake case
            linter_issue:rule_unused_variable - Variable 'Sum' is never used
            linter_issue:rule_naming_convention - 'Sum' should be in snake case
            linter_issue:formatting - Insert: `⏎` at line 2, column 1
            linter_issue:formatting - Insert: `····` at line 3, column 1
            linter_issue:formatting - Change: `··` to '⏎' at line 7, column 2
        """.trimIndent())
        assertThat(logger.output()).doesNotContain("Analyzing: test.rell... no issues found")
    }

    @Test
    fun testLintNoIssues() {
        LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--source-dir", testDir.resolve("src/test").toString()))
        assertThat(logger.output()).doesNotContain("test.rell")
    }

    @Test
    fun testFormatOptions() {
        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--formatter-options", testDir.resolve(".my_rell_format").toString()))
        }
        assertThat(logger.output()).contains("""
            |    linter_issue:formatting - Insert: `··` at line 3, column 1
            |    linter_issue:formatting - Delete: `··` from line 4, column 3
            |    linter_issue:formatting - Delete: `··` from line 5, column 3
            |    linter_issue:formatting - Delete: `··` from line 6, column 3
        """.trimMargin())
    }

    @Test
    fun testLintOptions() {
        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--linter-options", testDir.resolve(".my_rell_lint").toString()))
        }
        assertThat(logger.output()).contains("""
        main/module.rell
            linter_issue:rule_constant_detection - Variable 'x' is never modified, so it can be declared using 'val'
            linter_issue:rule_constant_detection - Variable 'Sum' is never modified, so it can be declared using 'val'
            linter_issue:rule_naming_convention - 'Hello' should be in snake case
            linter_issue:rule_unused_variable - Variable 'Sum' is never used
            linter_issue:rule_naming_convention - 'Sum' should be in snake case
            linter_issue:rule_quote_format - Use single quotes for "Hi!"
        """.trimIndent())
        assertThat(logger.output()).doesNotContain("test/test.rell")
    }

    @Test
    fun testFix() {
        val fixDir = testDir.resolve("fix").createDirectory()
        val fileContent = """
            module;
             query Hello() {
            var x = 5;
                  val y = 'wrong quotes';
                var z = 'more ' + y;
             return "Hi!";
            }  
        """.trimIndent()

        with(File(fixDir.toFile(), "src/main/module.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), ".rell_lint")) {
            writeText("""
                [*.rell]
                rule_quote_format=double
                rule_formatter=true
            """.trimIndent())
        }

        val settingsFile = File(fixDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
            """.trimIndent())
        }

        LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--fix"))
        val expectedContent = """
            module;
            
            query Hello() {
                val x = 5;
                val y = "wrong quotes";
                val z = "more " + y;
                return "Hi!";
            }
            
        """.trimIndent()

        assertThat(logger.output()).contains("Fixing: main/module.rell... fixed")
        assertThat(fixDir.resolve("src/main/module.rell").readText()).isEqualTo(expectedContent)
    }

    @Test
    fun testFixWithArgumentFilter() {
        val fixDir = testDir.resolve("fix").createDirectory()
        val fileContent = """
            module;
             query Hello() {
            var x = 5;
                  val y = 'wrong quotes';
                var z = 'more ' + y;
             return "Hi!";
            }  
        """.trimIndent()

        with(File(fixDir.toFile(), "src/main/module.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), "src/main/doNotFix.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), ".rell_lint")) {
            writeText("""
                [*.rell]
                rule_quote_format=double
                rule_formatter=true
            """.trimIndent())
        }

        val settingsFile = File(fixDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
            """.trimIndent())
        }

        LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--fix", "**/module.rell"))
        val expectedContent = """
            module;
            
            query Hello() {
                val x = 5;
                val y = "wrong quotes";
                val z = "more " + y;
                return "Hi!";
            }
            
        """.trimIndent()

        assertThat(logger.output()).contains("Fixing: main/module.rell... fixed")
        assertThat(logger.output()).doesNotContain("Fixing: main/doNotFix.rell... fixed")
        assertThat(fixDir.resolve("src/main/module.rell").readText()).isEqualTo(expectedContent)
        assertThat(fixDir.resolve("src/main/doNotFix.rell").readText()).isNotEqualTo(expectedContent)
    }


    @Test
    fun testLintWithArgumentFilter() {
        val fixDir = testDir.resolve("fix").createDirectory()
        val fileContent = """
            module;
             query Hello() {
            }  
        """.trimIndent()

        with(File(fixDir.toFile(), "src/main/module.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), "src/main/second.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), "src/main/doNotShow.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }


        with(File(fixDir.toFile(), ".rell_lint")) {
            writeText("""
                [*.rell]
                rule_quote_format=double
                rule_formatter=true
            """.trimIndent())
        }

        val settingsFile = File(fixDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
            """.trimIndent())
        }

        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "**/module.rell", "**/second.rell"))
        }
        assertThat(logger.output()).contains("main/module.rell")
        assertThat(logger.output()).contains("main/second.rell")
        assertThat(logger.output()).doesNotContain("main/doNotShow.rell")
    }

    @Test
    fun testLintWithArgumentFilterForDirectory() {
        val fixDir = testDir.resolve("fix").createDirectory()
        val noFixDir = testDir.resolve("noFix").createDirectory()
        val fileContent = """
            module;
             query Hello() {
            }  
        """.trimIndent()

        with(File(fixDir.toFile(), "src/main/module.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(fixDir.toFile(), "src/main/second.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }

        with(File(noFixDir.toFile(), "src/main/doNotShow.rell")) {
            parentFile.mkdirs()
            writeText(fileContent)
        }


        with(File(fixDir.toFile(), ".rell_lint")) {
            writeText("""
                [*.rell]
                rule_quote_format=double
                rule_formatter=true
            """.trimIndent())
        }

        val settingsFile = File(fixDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
            """.trimIndent())
        }

        assertThrows<RellCliExitException> {
            LintCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "**/fix/src/main/*"))
        }
        assertThat(logger.output()).contains("main/module.rell")
        assertThat(logger.output()).contains("main/second.rell")
        assertThat(logger.output()).doesNotContain("main/doNotShow.rell")
    }
}
