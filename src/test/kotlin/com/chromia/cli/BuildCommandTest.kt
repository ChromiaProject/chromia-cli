package com.chromia.cli

import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith

internal class BuildCommandTest {


    val testConsole = TestConsole()
    @RegisterExtension
    val command = CommandExtension(BuildCommand().context { console = testConsole })
    val dir get() = command.dir


    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<FileNotFoundException> {
            BuildCommand().parse(listOf())
        }
        assertEquals("config.yml (No such file or directory)", exception.message)
    }

    @Test
    fun testBridOutput() {
        command.parse(listOf("--show-brid"))
        testConsole.assertContains("hello 189C96B9373BCB777D8F63745956B8D59734B689F7DAB04D6E09CB32D0C53C57\n")
    }

    @Test
    fun testMultipleRun() {
        command.parse()
        command.parse()
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 2)
        assertTrue(File(dir, "build/hello.xml").exists())
        assertTrue(File(dir, "build/hello.gtv").exists())
    }

    @Test
    fun testSingleRun() {
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 2)
        assertTrue(File(dir, "build/hello.xml").exists())
        assertTrue(File(dir, "build/hello.gtv").exists())
    }
}
