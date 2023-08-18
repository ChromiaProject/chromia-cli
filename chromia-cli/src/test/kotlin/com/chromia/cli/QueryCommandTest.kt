package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.util.testData
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir


class QueryCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        testData(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()

    }

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
        val res = QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "hello", "arg1 -> 1"))
        assertThat(res.stderr).contains("invalid value for <args>: query must be done with named parameters in a dict")
    }

    @Test
    fun testMissingBrid() {
        val thrown = assertThrows<RuntimeException> {
            QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "--api-url", "http://localhost:7740", "hello"))
        }

        assertThat(thrown.message!!).contains("Could not auto-detect brid from")
    }
}