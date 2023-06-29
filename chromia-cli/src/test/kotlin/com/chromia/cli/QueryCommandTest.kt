package com.chromia.cli

import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingArgument
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import assertk.assertThat
import assertk.assertions.contains

class QueryCommandTest {
    //TODO add integration tests for querying the db
//    @Test
//    fun testOutput() {
//        QueryCommand().context {
//            console = object : TestConsole() {
//                override fun print(text: String, error: Boolean) {
//                    assertEquals(text, "\"Hello World!\"\n")
//                }
//            }
//        }.parse(listOf("hello_world"))
//    }

//    @Test
//    fun testBadRequest() {
//        QueryCommand().context {
//            console = object : TestConsole() {
//                override fun print(text: String, error: Boolean) {
//                    assertEquals(text, "Can not make a query: 400 Bad Request Query 'hello_world' failed: Wrong arguments: [arg1] instead of []\n")
//                }
//            }
//        }.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml"), "hello_world", "arg1=1"))
//    }


    private val command = QueryCommand()

    @Test
    fun testMissingQueryName() {
        val exception = assertFailsWith<MissingArgument> {
            command.parse(listOf())
        }
        assertEquals("Missing argument \"QUERYNAME\"", exception.message)
    }

    @Test
    fun testCanNotFindSettings() {
        val exception = assertFailsWith<BadParameterValue> {
            command.parse(listOf("--settings=missingFile.yml", "hello_world"))
        }
        assertThat(exception.message!!).contains("Invalid value for \"--settings\": missingFile.yml")
    }

    @Test
    fun testCanNotParseArgs() {
        val exception = assertFailsWith<BadParameterValue> {
            command.parse(listOf("--settings", dir!!.absolutePath.plus("/config.yml"), "hello_world", "arg1 -> 1"))
        }
        assertEquals("Invalid value for \"ARGS\": query must be done with named parameters in a dict", exception.message)
    }


    companion object {
        @TempDir
        @JvmField
        var dir: File? = null

        @JvmStatic
        @BeforeAll
        fun setup() {
            CreateRellDappCommand().parse(listOf("-d", dir!!.absolutePath))
            //StartCommand().parse(listOf("-s", dir!!.absolutePath.plus("/config.yml"), "--wipe"))
        }
    }
}