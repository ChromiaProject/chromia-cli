package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
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

    @Test
    fun testSubModuleSelectiveTest(@TempDir dir: Path) {

        with(File(dir.toFile(), "src/testDir/test.rell")) {
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
                    - testDir
            """.trimIndent())
        }
        val testConsole = TestConsole()
        TestCommand().context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--tests", "test_a"))
        testConsole.assertContains("\nSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL\n\n")
    }

    @Test
    fun testSubModuleAllTests(@TempDir dir: Path) {

        with(File(dir.toFile(), "src/testDir/test.rell")) {
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
                    - testDir
            """.trimIndent())
        }
        val testConsole = TestConsole()
        TestCommand().context { console = testConsole }.parse(listOf("-s", settings.absolutePath))
        testConsole.assertContains("\nSUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL\n\n")
    }

    @Test
    fun testWithDb(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                
                struct module_args { name; } // Makes sure module args are propagated from config when making a rell.test.tx()
                entity foo { name; }
                
                operation add_foo(name) { create foo(name); }
                query get_foo(name) = foo @? { name };
            """.trimIndent())
        }
        with(File(dir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;
                import ^.main.*;
                
                function test_get_foo() {
                  rell.test.tx().op(add_foo("bar")).run();
                  assert_not_null(get_foo("bar"));
                }
            """.trimIndent())
        }

        val settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                test:
                  modules: 
                    - test
                  moduleArgs:
                    main:
                      name: foo
                    non_existent: # Makes sure it is ok to specify "too many" nodes in the test context
                      foo: bar
            """.trimIndent())
        }
        val testConsole = TestConsole()
        TestCommand().context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--use-db"))
        testConsole.assertContains("\nSUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL\n\n")
    }
}
