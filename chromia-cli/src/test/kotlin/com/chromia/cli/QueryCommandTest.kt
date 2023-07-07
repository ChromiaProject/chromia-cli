package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.github.ajalt.clikt.testing.test
import java.io.File
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class QueryCommandTest {


    @Test
    fun testMissingQueryName() {
        val res = QueryCommand().test(listOf())
        assertThat(res.stderr).contains("missing argument <queryname>")
    }

    @Test
    fun testCanNotFindSettings() {
        val res = QueryCommand().test(listOf("--settings=missingFile.yml", "hello_world"))
        assertThat(res.stderr).contains("invalid value for --settings: file \"missingFile.yml")
    }

    @Test
    fun testCanNotParseArgs() {
        val res = QueryCommand().test(listOf("--settings", dir!!.absolutePath.plus("/config.yml"), "hello_world", "arg1 -> 1"))
        assertThat(res.stderr).contains("invalid value for <args>: query must be done with named parameters in a dict")
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