package com.chromia.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingArgument
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class QueryCommandTest {

    @Test
    fun testOutput() {
        QueryCommand().context {
            console = object : TestConsole() {
                override fun print(text: String, error: Boolean) {
                    assertEquals(text, "\"Hello World!\"\n")
                }
            }
        }.parse(listOf("hello_world"))
    }


    @Test
    fun missingQueryNameTest() {
        val exception = assertFailsWith<MissingArgument> {
            QueryCommand().parse(listOf())
        }
        assertEquals("Missing argument \"QUERYNAME\"", exception.message)
    }

    @Test
    fun canNotFindSettingsTest() {
        val exception = assertFailsWith<BadParameterValue> {
            QueryCommand().parse(listOf("--settings=missingFile.yml", "hello_world"))
        }
        assertEquals("Invalid value for \"--settings\": missingFile.yml (No such file or directory)", exception.message)
    }


    companion object {
        @TempDir
        @JvmField
        var dir: File? = null

        @JvmStatic
        @BeforeAll
        fun setup() {
            InitCommand().parse(listOf("-d", dir!!.absolutePath))
            StartCommand().parse(listOf("-s", dir!!.absolutePath.plus("/config.yml"), "--wipe"))
        }
    }
}