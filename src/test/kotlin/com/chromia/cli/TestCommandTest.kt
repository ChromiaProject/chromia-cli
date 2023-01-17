package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class TestCommandTest {

    @Test
    fun testFilter(@TempDir dir: Path) {

        with(File(dir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;
                
                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }

        val settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                test:
                  modules: 
                    - test
            """.trimIndent())
        }
        val testConsole = TestConsole()
        TestCommand().context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--tests", "test_a"))
        testConsole.assertContains("\nSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL\n\n")
    }
}
