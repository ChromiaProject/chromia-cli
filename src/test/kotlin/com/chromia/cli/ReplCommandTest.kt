package com.chromia.cli

import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException
import kotlin.test.assertFailsWith

class ReplCommandTest {

    @TempDir
    @JvmField
    var dir: File? = null

    @BeforeEach
    fun setup() {
        InitCommand().parse(listOf("-d", dir!!.absolutePath))
    }

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
            ReplCommand().parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml")))
        }
        assertEquals("Missing option \"--module\"", exception.message)
    }

    @Test
    fun testCanNotConnectToDb() {
        ReplCommand().context {
            console = object : TestConsole() {
                override fun print(text: String, error: Boolean) {
                    assertEquals(text, "Connection to localhost:5432 refused. Check that the hostname and port are correct and that the postmaster is accepting TCP/IP connections.\n")
                }
            }
        }.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml"),"--module=main",  "--use-sql"))
    }
}