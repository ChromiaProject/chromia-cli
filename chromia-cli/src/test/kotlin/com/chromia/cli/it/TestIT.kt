package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration

class TestIT {
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
    fun testBlockchainWithFileSpecificAbsolute() {
        TestProcess.Builder("test", "-s", settingsFile.absolutePath, "-bc", "hello",
                        "--file", testFile.absolutePath, "--no-db")
                .setWorkingDir(projectDir.toFile())
                .start {
                    it.waitUntil("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL", Duration.ofSeconds(10))
                }
    }

    @Test
    fun testBlockchainWithFileSpecificRelative() {
        TestProcess.Builder("test", "-s", settingsFile.absolutePath, "-bc", "hello",
                        "--file", testFile.relativeTo(projectDir.toFile()).toString(), "--no-db")
                .setWorkingDir(projectDir.toFile())
                .start {
                    it.waitUntil("SUMMARY: 0 FAILED / 2 PASSED / 2 TOTAL", Duration.ofSeconds(10))
                }
    }

    @Test
    fun testBlockchainWithDirSpecificAbsolute() {
        TestProcess.Builder("test", "-s", settingsFile.absolutePath, "-bc", "hello",
                        "--file", testDir.absolutePath, "--no-db")
                .setWorkingDir(projectDir.toFile())
                .start {
                    it.waitUntil("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL", Duration.ofSeconds(10))
                }
    }

    @Test
    fun testBlockchainWithDirSpecificRelative() {
        TestProcess.Builder("test", "-s", settingsFile.absolutePath, "-bc", "hello",
                        "--file",  testDir.relativeTo(projectDir.toFile()).toString(), "--no-db")
                .setWorkingDir(projectDir.toFile())
                .start {
                    it.waitUntil("SUMMARY: 0 FAILED / 4 PASSED / 4 TOTAL", Duration.ofSeconds(10))
                }
    }
}
