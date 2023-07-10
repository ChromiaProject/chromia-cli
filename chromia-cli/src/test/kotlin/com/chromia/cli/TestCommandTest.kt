package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class TestCommandTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
            """.trimIndent())
        }

        with(File(dir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }

        settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
                        test:
                            modules:
                                - testDir
                test:
                  modules:
                    - test
            """.trimIndent())
        }

        with(File(dir.toFile(), "src/testDir/bar.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_c() {}
                function test_d() {}
            """.trimIndent())
        }

        with(File(dir.toFile(), "src/testDir/foo.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }
        testDir = dir
    }

    @Test
    fun testFilter() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--tests", "test_a", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL")
    }

    @Test
    fun testFilterWildCard() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--tests", "test_*", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testBlockchain() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testModule() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-m", "test", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testSubModuleSelectiveTest() {
        File(testDir.toFile(), "config.yml").apply {
            writeText("""
                test:
                  modules:
                    - testDir
            """.trimIndent())
        }

        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--tests", "test_a", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL")
    }

    @Test
    fun testSubModuleAllTests() {
        File(testDir.toFile(), "config.yml").apply {
            writeText("""
                test:
                  modules:
                    - testDir
            """.trimIndent())
        }
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testWithDb() {
        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;

                struct module_args { name; } // Makes sure module args are propagated from config when making a rell.test.tx()
                entity foo { name; }

                operation add_foo(name) { create foo(name); }
                query get_foo(name) = foo @? { name };
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/test_ops.rell")) {
            writeText("""
                module;
                operation add_foo(name, pubkey) {} // Conflict
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/test.rell")) {
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

        File(testDir.toFile(), "config.yml").apply {
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
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--use-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL")
    }

    @Test
    fun failingTest() {
        with(File(testDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db"))
        }
        assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 1 PASSED / 2 TOTAL")
    }

    @Test
    fun testBlockchainTestScope() {
        with(File(testDir.toFile(), "src/development.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;

                struct module_args { name; age: integer;}
                entity foo { name; }

                operation add_foo(name) { create foo(name); }
                query get_foo(name) = foo @? { name };
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/moduleA/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;
                import ^^.development.*;

                function test_get_foo() {
                  rell.test.tx().op(add_foo("bar")).run();
                  assert_not_null(get_foo("bar"));
                }
            """.trimIndent())
        }
        with(File(testDir.toFile(), "src/moduleB/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }
        File(testDir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  foo_chain_dev:
                    module: development
                    moduleArgs:
                      development:
                        age: 100
                    test:
                      modules:
                        - moduleA.test
                        - moduleB.test
                      moduleArgs:
                        development:
                          name: foo
                        non_existent:
                          foo: bar
                  foo_chain:
                    module: production
                    test:
                      modules:
                        - non_existent_prod_tests
            """.trimIndent())
        }
        TestCommand().context { terminal = testTerminal }.parse(
                listOf("-s", settingsFile.absolutePath, "--use-db", "--blockchain", "foo_chain_dev")
        )
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 3 PASSED / 3 TOTAL")
    }
}
