package com.chromia.cli.command.tools

import com.chromia.build.tools.testData
import com.github.ajalt.clikt.core.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LibModelGenerateCommandTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `test valid git SSH URL validation`() {
        val command = LibModelGenerateCommand()

        assertDoesNotThrow {
            command.parse(listOf(
                    "--name", "test-lib",
                    "--library-source", tempDir.toString(),
                    "--registry", "git@github.com:user/repo.git"
            ))
        }
    }

    @Test
    fun `test valid git HTTPS URL validation`() {
        val command = LibModelGenerateCommand()

        assertDoesNotThrow {
            command.parse(listOf(
                    "--name", "test-lib",
                    "--library-source", tempDir.toString(),
                    "--registry", "https://github.com/user/repo.git"
            ))
        }
    }

    @Test
    fun `test invalid git URL validation`() {
        val command = LibModelGenerateCommand()

        assertFailsWith<BadParameterValue> {
            command.parse(listOf(
                    "--name", "test-lib",
                    "--library-source", tempDir.toString(),
                    "--registry", "invalid-url"
            ))
        }
    }

    @Test
    fun `test missing required library source`() {
        val command = LibModelGenerateCommand()

        assertFailsWith<MissingOption> {
            command.parse(listOf(
                    "--name", "test-lib",
                    "--registry", "git@github.com:user/repo.git"
            ))
        }
    }

    @Test
    fun `test command with rell project`() {
        testData(tempDir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module;""")
            addSourceFile("lib/my/test/module.rell", """@test module;""")
        }
        val expectedRid = "635D662C225007E8BB706C71AE695B5A86E8398C43096B0C42D6812CF0B5C02D"


        LibModelGenerateCommand().context { terminal = testTerminal }.parse(
                listOf(
                        "--name", "test-lib",
                        "--library-source", tempDir.toString(),
                        "--registry", "git@github.com:user/repo.git",
                        "--tag-or-branch", "main"
                )
        )

        assertTrue(logger.output().contains("test-lib:"))
        assertTrue(logger.output().contains("registry: git@github.com:user/repo.git"))
        assertTrue(logger.output().contains("path: $tempDir"))
        assertTrue(logger.output().contains("tagOrBranch: main"))
        assertTrue(logger.output().contains("rid: $expectedRid"))
        assertTrue(logger.output().contains("insecure: false"))
    }

    @Test
    fun `test Git URL regex matches valid formats`() {
        val command = LibModelGenerateCommand()

        val validUrls = listOf(
                "git@github.com:user/repo.git",
                "git@gitlab.com:org/group/repo.git",
                "https://chromaway.com/user/repo.git",
                "https://bitbucket.com/org/group/repo.git",
                "http://github.com/user/repo.git",
                "https://github.com:8080/user/repo.git",
                "git@github.com:user/repo-name.git",
                "git@company-git.com:project/repo.git",
                "https://git.company.com/group/repo.git",
                "git@gitlab.company.com:group/subgroup/repo.git",
                "git@example.com:2222:user/repo.git"
        )

        validUrls.forEach { url ->
            assertDoesNotThrow {
                command.parse(listOf(
                        "--name", "test-lib",
                        "--library-source", tempDir.toString(),
                        "--registry", url
                ))
            }
        }
    }

    @Test
    fun `test Git URL regex rejects invalid formats`() {
        val command = LibModelGenerateCommand()

        val invalidUrls = listOf(
                "",
                "not a git url",
                "git@github",
                "https://bitbucket.com",
                "git@github.com:user",
                "ftp://github.com/user/repo.git",
                "git@github.com::user/repo.git",
                "git@github.com:user//repo.git",
                "git@github.com:user/repo.git/",
                "git@github.com:/user/repo.git",
                "https://github.com/repo",
                "git@.com:user/repo.git",
                "git@github.com:user/repo.git.git",
                "git@github.com:user/repo..git",
                "git@github.com:user/sp ace/repo.git",
                "git@github.com:user/@repo.git",
        )

        invalidUrls.forEach { url ->
            assertFailsWith<BadParameterValue> {
                command.parse(listOf(
                        "--name", "test-lib",
                        "--library-source", tempDir.toString(),
                        "--registry", url
                ))
            }
        }
    }

}