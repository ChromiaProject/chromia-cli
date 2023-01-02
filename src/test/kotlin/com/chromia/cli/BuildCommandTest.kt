package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith

internal class BuildCommandTest {

    @TempDir
    @JvmField
    var dir: File? = null

    val testConsole = TestConsole()
    val command = BuildCommand().context { console = testConsole }

    @BeforeEach
    fun setup() {
        InitCommand().parse(listOf("-d", dir!!.absolutePath))
    }

    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<FileNotFoundException> {
            command.parse(listOf())
        }
        assertEquals("config.yml (No such file or directory)", exception.message)
        assertEquals(0, testConsole.out.size)
    }

    @Test
    fun testBridOutput() {
        command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml"), "--show-brid"))
        testConsole.assertContains("hello 189C96B9373BCB777D8F63745956B8D59734B689F7DAB04D6E09CB32D0C53C57\n")
    }

    @Test
    fun testMultipleRun() {
        command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml")))
        command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml")))
        command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml")))

        assertEquals(File(dir, "build").list()?.size ?: 0, 2)
        assertTrue(File(dir, "build/hello.xml").exists())
        assertTrue(File(dir, "build/hello.gtv").exists())
    }

    @Test
    fun testSingleRun() {
        command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml")))

        assertEquals(File(dir, "build").list()?.size ?: 0, 2)
        assertTrue(File(dir, "build/hello.xml").exists())
        assertTrue(File(dir, "build/hello.gtv").exists())
    }
}