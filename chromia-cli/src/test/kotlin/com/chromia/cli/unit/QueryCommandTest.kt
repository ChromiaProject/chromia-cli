package com.chromia.cli.unit

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.QueryCommand
import com.chromia.cli.it.TestDataCreator
import com.chromia.cli.util.TestClient
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class QueryCommandTest {

    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        testDir = dir
        settingsFile = testDir.resolve("config.yml").toFile()

    }

    @Test
    fun testMissingQueryName() {
        val res = QueryCommand().test(listOf())
        assertThat(res.stderr).contains("missing argument <queryname>")
    }

    @Test
    fun testCanNotFindSettings() {
        val res = QueryCommand().test(listOf("--settings=missingFile.yml", "hello_world"))
        assertThat(res.stderr).contains("invalid value for --settings: missingFile.yml")
    }

    @Test
    fun testCanNotParseArgs() {
        val res = QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "hello", "arg1 -> 1"))
        assertThat(res.stderr).contains("invalid value for <args>: query must be done with named parameters in a dict")
    }

    @Test
    fun testMissingBrid() {
        val thrown = assertThrows<IllegalArgumentException> {
            QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "--api-url", "http://localhost:7740", "hello"))
        }

        assertThat(thrown.message!!).contains("Wrong size of Blockchain RID, was 0 should be 32 (64 characters)")
    }
}