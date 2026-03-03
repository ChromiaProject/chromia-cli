package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import com.chromia.build.tools.testData
import com.chromia.cli.tools.formatter.danger
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.tools.formatter.success
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.redundent.kotlin.xml.CDATAElement
import org.redundent.kotlin.xml.Node
import org.redundent.kotlin.xml.parse
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists

internal class TestCommandTest {

    private val logger = TerminalRecorder(width = 1000, outputInteractive = true)
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var projectDir: Path
    private lateinit var settingsFile: File
    private lateinit var testDir: File
    private lateinit var testFile: File

    @BeforeEach
    fun setup() {
        with(File(projectDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
            """.trimIndent())
        }

        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }

        settingsFile = File(projectDir.toFile(), "chromia.yml").apply {
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

        with(File(projectDir.toFile(), "src/testDir/bar.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_c() {}
                function test_d() {}
            """.trimIndent())
        }

        testDir = File(projectDir.toFile(), "src/testDir").apply {
            mkdirs()
        }

        testFile = File(testDir, "foo.rell").apply {
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }
    }

    @Test
    fun testRunningAllTests() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL") //Blockchain test
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL") //Unit test
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
    fun testBlockchainWithModuleSpecific() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "-m", "testDir.foo", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testBlockchainWithDirModuleSpecific() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "-m", "testDir", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testBlockchainWithModuleNotInScope() {
        val throwable = assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "-m", "test,test2", "--no-db"))
        }
        assertThat(throwable.message!!).contains("No tests to run")
    }

    @Test
    fun testMultipleBlockchainsWithModule() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
                        test:
                            modules:
                                - testDir
                    hello2:
                        module: main
                        test:
                            modules:
                                - testDir
            """.trimIndent())
        }

        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "-bc", "hello2", "-m", "testDir.foo", "--no-db"))
        assertThat(logger.output()).contains("Running tests for chain: hello\n")
        assertThat(logger.output()).contains("Running tests for chain: hello2\n")
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testMultipleBlockchainsWithoutModule() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    hello:
                        module: main
                        test:
                            modules:
                                - testDir
                    hello2:
                        module: main
                        test:
                            modules:
                                - testDir
            """.trimIndent())
        }


        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "-bc", "hello2", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testModule() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-m", "test", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testSubModuleSelectiveTest() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  main:
                    module: main
                test:
                  modules:
                    - testDir
            """.trimIndent())
        }

        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--tests", "test_a", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL")
    }

    @Test
    fun testUnitMultipleModules() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  main:
                    module: main
                test:
                  modules:
                    - testDir
            """.trimIndent())
        }
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-m", "testDir.bar,testDir.foo", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testUnitMultipleModulesOnlyRunsTestsOnceWhenDuplicateInvocations() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  main:
                    module: main
                test:
                  modules:
                    - testDir
            """.trimIndent())
        }
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-m", "testDir,testDir.foo", "--no-db"))
        assertThat(logger.output()).contains("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL")
    }

    @Test
    fun testSubModuleAllTests() {
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  main:
                    module: main
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
        with(File(projectDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;

                struct module_args { name; } // Makes sure module args are propagated from config when making a rell.test.tx()
                entity foo { name; }

                operation add_foo(name) { create foo(name); }
                query get_foo(name) = foo @? { name };
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/test_ops.rell")) {
            writeText("""
                module;
                operation add_foo(name, pubkey) {} // Conflict
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/test.rell")) {
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

        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains: 
                  main:
                    module: main
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
        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--fail-on-error"))
        }
        assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 1 PASSED / 2 TOTAL")
    }

    @Test
    fun failingTestColors() {
        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        val testCommand = TestCommand()
        assertThrows<CliktError> {
            testCommand.context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--fail-on-error"))
        }

        val output = logger.output()
        assertThat(output).contains("${testCommand.success("OK")} test:test_a")
        assertThat(output).contains(testCommand.info("FAILED TESTS:"))
        assertThat(output).contains("${testCommand.danger("FAILED")} test:test_b")
        assertThat(output).contains(testCommand.danger("***** FAILED *****"))
        assertThat(output).contains("${testCommand.danger("Error:")} System function 'rell.test.assert_equals'")
        assertThat(output).contains(testCommand.info("Stack trace:"))
        assertThat(output).contains(testCommand.danger("Expected:"))
        assertThat(output).contains(testCommand.success("Actual:"))
    }

    @Test
    fun okTestsColors() {
        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() { assert_equals(1, 1); }
            """.trimIndent())
        }

        val testCommand = TestCommand()
        testCommand.context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--fail-on-error"))

        val output = logger.output()
        assertThat(output).contains("${testCommand.success("OK")} test:test_a")
        assertThat(output).contains("${testCommand.success("OK")} test:test_b")
        assertThat(output).contains(testCommand.success("***** OK *****"))
    }

    @Test
    fun failingTestStopOnError() {
        settingsFile = File(projectDir.toFile(), "chromia.yml").apply {
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
                  failOnError: true
            """.trimIndent())
        }

        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_b() { assert_equals(1, 2); }
                function test_a() {}
            """.trimIndent())
        }

        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db"))
        }
        assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 0 PASSED / 1 TOTAL")
    }

    @Test
    fun testFailingTestContinuesWithGlobalOverride() {
        settingsFile = File(projectDir.toFile(), "chromia.yml").apply {
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
                  failOnError: true
            """.trimIndent())
        }

        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_b() { assert_equals(1, 2); }
                function test_a() {}
            """.trimIndent())
        }

        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--fail-on-error=false"))
        }
        assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 1 PASSED / 2 TOTAL")
    }

    @Test
    fun failingTestStopOnErrorFlag() {
        settingsFile = File(projectDir.toFile(), "chromia.yml").apply {

            with(File(projectDir.toFile(), "src/test.rell")) {
                parentFile.mkdirs()
                writeText("""
                @test module;

                function test_b() { assert_equals(1, 2); }
                function test_a() {}
            """.trimIndent())
            }

            assertThrows<CliktError> {
                TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--fail-on-error"))
            }
            assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 0 PASSED / 1 TOTAL")
        }
    }

    @Test
    fun testBlockchainTestScopeFailOnErrorStopsGlobally() {
        with(File(projectDir.toFile(), "src/development.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/production.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/moduleA/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_c() {assert_equals(1, 2);}
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/moduleB/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;
                
                function test_b() {}
                function test_a() {}
            """.trimIndent())
        }
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  a:
                    module: development
                    test:
                      modules:
                        - moduleA.test
                  b:
                    module: production
                    test:
                      modules:
                        - moduleB.test
            """.trimIndent())
        }
        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(
                    listOf("-s", settingsFile.absolutePath, "--blockchain", "a", "--blockchain", "b", "--fail-on-error=true")
            )
        }
        assertThat(logger.output()).contains("SUMMARY: 1 FAILED / 0 PASSED / 1 TOTAL")
        assertThat(logger.output()).doesNotContain("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL")
    }

    @Test
    fun testBlockchainTestScope() {
        with(File(projectDir.toFile(), "src/development.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;

                struct module_args { name; age: integer;}
                entity foo { name; }

                operation add_foo(name) { create foo(name); }
                query get_foo(name) = foo @? { name };
            """.trimIndent())
        }
        with(File(projectDir.toFile(), "src/moduleA/test.rell")) {
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
        with(File(projectDir.toFile(), "src/moduleB/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_a() {}
                function test_b() {}
            """.trimIndent())
        }
        File(projectDir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  foo_chain_dev:
                    module: development
                    moduleArgs:
                      development:
                        age: 100
                    config:
                        gtx:
                            modules:
                                - non.existing.module.should.be.FineGTXmodule # This could for example be ICMF which only works at runtime
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

    @Test
    fun testTestReportSuccess() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db", "--test-report", "--test-report-dir", projectDir.toString()))
        val testReport = parse(projectDir.resolve("rell-unit-tests.xml").toFile())
        assertThat(testReport.nodeName).isEqualTo("testsuite")
        assertThat(testReport.attributes["name"]).isEqualTo("rell")
        assertThat(testReport.attributes["failures"]).isEqualTo("0")
        assertThat(testReport.attributes["tests"]).isEqualTo("2")
        assertThat(testReport.attributes["version"]).isEqualTo("3.0")
        val case = testReport.children.filterIsInstance<Node>().first()
        assertThat(case.nodeName).isEqualTo("testcase")
        assertThat(case.attributes["name"]).isEqualTo("test_a")
        assertThat(case.attributes["classname"]).isEqualTo("test")
        assertThat(projectDir.resolve("rell-unit-tests-sql.html").exists()).isTrue()
    }

    @Test
    fun testTestReportFailure() {
        with(File(projectDir.toFile(), "src/test.rell")) {
            parentFile.mkdirs()
            writeText("""
                @test module;

                function test_b() { assert_equals(1, 2); }
            """.trimIndent())
        }

        assertThrows<CliktError> {
            TestCommand().context { terminal = testTerminal }.parse(
                    listOf("-s", settingsFile.absolutePath, "--no-db", "--test-report", "--test-report-dir", projectDir.toString()))
        }
        val testReport = parse(projectDir.resolve("rell-unit-tests.xml").toFile())
        assertThat(testReport.nodeName).isEqualTo("testsuite")
        assertThat(testReport.attributes["name"]).isEqualTo("rell")
        val case = testReport.children.filterIsInstance<Node>().first()
        assertThat(case.nodeName).isEqualTo("testcase")
        assertThat(case.attributes["name"]).isEqualTo("test_b")
        assertThat(case.attributes["classname"]).isEqualTo("test")
        val failure = case.children.filterIsInstance<Node>().first()
        assertThat(failure.nodeName).isEqualTo("failure")
        assertThat(failure.attributes["message"]).isEqualTo("System function 'rell.test.assert_equals': expected <2> but was <1>")
        assertThat(failure.children.filterIsInstance<CDATAElement>().first().text.trim()).isEqualTo("System function 'rell.test.assert_equals': expected <2> but was <1>\n" +
                "\tat test:test_b(test.rell:3)")
        assertThat(projectDir.resolve("rell-unit-tests-sql.html").exists()).isTrue()
    }

    @Test
    fun testBlockchainTestReport() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-bc", "hello", "--no-db", "--test-report", "--test-report-dir", projectDir.toString()))
        assertThat(projectDir.resolve("hello-tests.xml").exists()).isTrue()
        assertThat(projectDir.resolve("hello-tests-sql.html").exists()).isTrue()
    }

    @Test
    fun testSqlHtmlReportNotCreatedWithoutTestReportFlag() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--no-db"))
        assertThat(projectDir.resolve("rell-unit-tests-sql.html").exists()).isFalse()
        assertThat(projectDir.resolve("hello-tests-sql.html").exists()).isFalse()
    }

    @Test
    fun testModuleTestReport() {
        TestCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "-m", "test", "--no-db", "--test-report-dir", projectDir.toString()))
        assertThat(projectDir.resolve("test-tests.xml").exists())
    }

    @Test
    fun testIccfDoesNothing() {
        testData(projectDir) {
            config {
                blockchains(
                        """
                           blockchains:
                                hello:
                                    module: main
                                    test:
                                        modules:
                                            - testDir
                                    config:
                                        gtx:
                                            modules:
                                                - net.postchain.d1.iccf.IccfGTXModule
                        """.trimIndent()
                )
            }
            addSourceFile("testDir/foo.rell", """
                @test module;
                
                function test_iccf_success() {
                    rell.test.tx()
                        .op(gtx_operation(name = "iccf_proof", args = [x"AAAA".to_gtv(), x"AB12".to_gtv(), x"".to_gtv()]).to_test_op())
                        .run();
                }

            """.trimIndent())
        }
        val res = TestCommand().test("-s ${settingsFile.absolutePath}")
        assertThat(res.output).contains("OK: testDir.foo:test_iccf_success")
    }

    @Test
    fun testIccfDoesNotWorkOnUnitTest() {
        testData(projectDir) {
            config {
                blockchains(
                        """
                           blockchains:
                                hello:
                                    module: main
                                    config:
                                        gtx:
                                            modules:
                                                - net.postchain.d1.iccf.IccfGTXModule
                        """.trimIndent()
                )
                test("""
                    test:
                        modules:
                          - test
                """.trimIndent())
            }
            addSourceFile("test.rell", """
                @test module;
                
                function test_iccf_success() {
                    rell.test.tx()
                        .op(gtx_operation(name = "iccf_proof", args = [x"AAAA".to_gtv(), x"AB12".to_gtv(), x"".to_gtv()]).to_test_op())
                        .run();
                }

            """.trimIndent())
        }
        val res = TestCommand().test("-s ${settingsFile.absolutePath}")
        assertThat(res.output).contains("Unknown operation: iccf_proof")
    }

    @Test
    fun `moduleOfFile with Rell file`() {
        val module = moduleOfFile(Path.of("root", "src"), Path.of("root", "src", "main", "test.rell"))
        assertThat(module).isEqualTo("main.test")
    }

    @Test
    fun `moduleOfFile with directory`() {
        val module = moduleOfFile(Path.of("root", "src"), Path.of("root", "src", "main", "test"))
        assertThat(module).isEqualTo("main.test")
    }
}
