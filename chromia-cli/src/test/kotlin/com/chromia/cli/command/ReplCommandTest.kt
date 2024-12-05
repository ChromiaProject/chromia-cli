package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.cli.util.InitExtension
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File

class ReplCommandTest {
    @JvmField
    @RegisterExtension
    val command = InitExtension()
    val dir get() = command.projectDir

    @Test
    fun testCanNotConnectToDb() {
        EnvironmentVariables("CHR_DB_URL", "jdbc:postgresql://invalidhost/postgres").execute {
            assertThrows<PSQLException> {
                ReplCommand().test("--module=main --use-db -s ${dir.absolutePath}/chromia.yml")
            }
        }
    }

    @Test
    fun testCanNotConnectToDbWithoutSettings() {
        EnvironmentVariables("CHR_DB_URL", "").execute {
            val res = ReplCommand().test("--use-db")
            assertThat(res.statusCode).isEqualTo(1)
            assertThat(res.stderr).contains("To correctly connect to the database, specifying the settings file is required")
        }
    }

    @Test
    fun commandLineInput() {
        val res = ReplCommand().test("-c '5+5'")
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.output).isEqualTo("10\n")
    }

    @Test
    fun commandLineInputWithCompileError() {
        val res = ReplCommand().test("-c 'print(17); bogus(); print(18)'")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).isEqualTo("""
            <console>(1:12) ERROR: Unknown name: 'bogus'

        """.trimIndent())
    }

    @Test
    fun commandLineInputWithRuntimeError() {
        val res = ReplCommand().test("""-c 'print(17); require(false, "the error"); print(18)'""")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).isEqualTo("""
            17
            Run-time error: the error
                    at :<console>(<console>:1)

        """.trimIndent())
    }

    @Test
    fun `default format`() {
        val res = ReplCommand().test("""-c '["One": 1, "Two": 2, "Three": 3]'""")
        assertThat(res.output).isEqualTo("""["One": 1, "Three": 3, "Two": 2]
            |
        """.trimMargin())
    }

    @Test
    fun `raw format`() {
        val res = ReplCommand().test("""--output-format raw -c '["One": 1, "Two": 2, "Three": 3]'""")
        assertThat(res.output).isEqualTo("""
            One=1
            Two=2
            Three=3
            
        """.trimIndent())
    }

    @Test
    fun `json format`() {
        val res = ReplCommand().test("""--output-format json -c '[\"One\": 1, \"Two\": 2, \"Three\": 3]'""")
        assertThat(res.output).isEqualTo("""
            {
              "One": 1,
              "Three": 3,
              "Two": 2
            }
            
            """.trimIndent())
    }

    @Test
    fun `xml format`() {
        val res = ReplCommand().test("""-f xml -c '[\"One\": 1, \"Two\": 2, \"Three\": 3]'""")
        assertThat(res.output).isEqualTo("""
            <dict>
                <entry key="One">
                    <int>1</int>
                </entry>
                <entry key="Three">
                    <int>3</int>
                </entry>
                <entry key="Two">
                    <int>2</int>
                </entry>
            </dict>
            
            """.trimIndent())
    }

    //TODO update assertion message when there is a proper warning message when a operation is called from repl
    @Test
    fun commandLineInputOperation() {
        with(File(dir, "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                
                object my_name {
                  mutable name= "World";
                 }
                
                operation set_name(name) {
                  my_name.name = name;
                }
            """.trimIndent())
        }
        val res = ReplCommand().test("--module=main -s ${dir.absolutePath}/chromia.yml -c 'set_name(\"foo\")'")
        assertThat(res.output).contains("Type rell.test.op cannot be converted to Gtv. Switch to a different output format.")
    }

    @Test
    fun commandLineInputStatusCode() {
        val res = ReplCommand().test("-c 'non_existing_entity @ {}'")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).contains("Unknown name: 'non_existing_entity'")
    }

    @Test
    fun nonInteractiveReplSession() {
        val recorder = TerminalRecorder(inputInteractive = false, outputInteractive = false)
        recorder.inputLines = mutableListOf("5+5")
        ReplCommand().context {
            terminal = Terminal(terminalInterface = recorder)
        }.parse(listOf())
        assertThat(recorder.stdout()).isEqualTo("10\n")
    }

    @Test
    @Disabled("Cannot send input to JLine")
    fun interactiveReplSession() {
        val recorder = TerminalRecorder(inputInteractive = true, outputInteractive = true)
        recorder.inputLines = mutableListOf("5+5", "\\q")
        ReplCommand().context {
            terminal = Terminal(terminalInterface = recorder)
        }.parse(listOf())
        assertThat(recorder.stdout()).isEqualTo("10\n")
    }

    @Test
    fun scriptFile() {
        with(File(dir, "script.rell")) {
            writeText("""
                print("Foo bar");
                5+6;
                print("Bar foo");                
            """.trimIndent())
        }
        val res = ReplCommand().test("${dir.absolutePath}/script.rell")
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.output).isEqualTo("""
            Foo bar
            11
            Bar foo
            
        """.trimIndent())
    }

    @Test
    fun scriptFileIgnoresShebang() {
        with(File(dir, "script.rell")) {
            writeText("""
                #!/usr/bin/env -S chr repl
                print("Foo bar");
                5+6;
                print("Bar foo");                
            """.trimIndent())
        }
        val res = ReplCommand().test("${dir.absolutePath}/script.rell")
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.output).isEqualTo("""
            Foo bar
            11
            Bar foo
            
        """.trimIndent())
    }

    @Test
    fun scriptFileWithArguments() {
        with(File(dir, "script.rell")) {
            writeText("""
                #!/usr/bin/env -S chr repl
                print(args.size());
                print(args[0]);
                print(args[1]);
                print(args[2]);
            """.trimIndent())
        }
        val res = ReplCommand().test("""${dir.absolutePath}/script.rell one "foo bar" three""")
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.output).isEqualTo("""
            3
            one
            foo bar
            three
            
        """.trimIndent())
    }

    @Test
    fun scriptFileWithCompileError() {
        with(File(dir, "script.rell")) {
            writeText("""
                #!/usr/bin/env -S chr repl
                print("Foo bar");
                bogus();
                print("Bar foo");                
            """.trimIndent())
        }
        val res = ReplCommand().test("${dir.absolutePath}/script.rell")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).isEqualTo("""
            Foo bar
            <console>(1:1) ERROR: Unknown name: 'bogus'

        """.trimIndent())
    }

    @Test
    fun scriptFileWithRuntimeError() {
        with(File(dir, "script.rell")) {
            writeText("""
                #!/usr/bin/env -S chr repl
                print("Foo bar");
                require(false, "the error");
                print("Bar foo");                
            """.trimIndent())
        }
        val res = ReplCommand().test("${dir.absolutePath}/script.rell")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).isEqualTo("""
            Foo bar
            Run-time error: the error
                    at :<console>(<console>:1)

        """.trimIndent())
    }

    @Test
    fun scriptFileNotFound() {
        val res = ReplCommand().test("${dir.absolutePath}/not_there.rell")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.stderr).contains("does not exist")
    }

    @Test
    fun scriptFileAndCommandIsNotAllowed() {
        with(File(dir, "script.rell")) {
            writeText("""
                print("Foo bar");
                print("Bar foo");                
            """.trimIndent())
        }
        val res = ReplCommand().test("-c 'print(17)' ${dir.absolutePath}/script.rell")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.stderr).contains("Cannot use -c when specifying script file")
    }
}
