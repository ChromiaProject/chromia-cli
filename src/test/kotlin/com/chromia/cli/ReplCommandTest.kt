package com.chromia.cli

import com.chromia.cli.QueryCommandTest.Companion.dir
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.InitExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith

class ReplCommandTest {

    val testConsole = TestConsole()

    @RegisterExtension
    private val command = CommandExtension(ReplCommand().context { console = testConsole })
    val dir get() = command.dir

    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<FileNotFoundException> {
            ReplCommand().parse(listOf())
        }
        assertEquals("config.yml (No such file or directory)", exception.message)
    }

    @Test
    fun testCanNotFindModule() {
        val exception = assertFailsWith<MissingOption> {
            command.parse()
        }
        assertEquals("Missing option \"--module\"", exception.message)
    }

    @Test
    fun testCanNotConnectToDb() {
        command.parse(listOf("--module=main", "--use-sql"))
        testConsole.assertContains("Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.\n")
    }
}
