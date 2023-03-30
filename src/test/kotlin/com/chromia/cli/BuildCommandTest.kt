package com.chromia.cli

import assertk.assert
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import kotlin.test.assertFailsWith

internal class BuildCommandTest {

    val testConsole = TestConsole()

    @RegisterExtension
    val command = CommandExtension(BuildCommand().context { console = testConsole })
    val dir get() = command.dir

    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<FileNotFound> {
            BuildCommand().parse(listOf())
        }
        assertTrue(exception.message?.contains("config.yml not found") ?: false)
    }

    @Test
    fun testBridOutput() {
        command.parse(listOf("--show-brid"))
        testConsole.assertContains("hello 340208CF6BA6D05932B74BFDEE54C60AC4015CF742D97449B6A2A8687E5CDFAF\n")
    }

    @Test
    fun testMultipleRun() {
        command.parse()
        command.parse()
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun testSingleRun() {
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun failsValidation() {
        File(dir, "config.yml").writeText("""
            blockchains:
              hello:
                module: main
                moduleArgs: 
                  main:
                    my_args: "wrong_type"
        """.trimIndent())
        File(dir, "src/main.rell").writeText("""
            module;
            struct module_args { my_args: integer; }
        """.trimIndent())
        val e = assertFailsWith<CliktError> { command.parse() }
        assertTrue(e.message!!.contains("Module initialization failed: Decoding type 'integer': expected INTEGER, actual STRING"))
    }
}
