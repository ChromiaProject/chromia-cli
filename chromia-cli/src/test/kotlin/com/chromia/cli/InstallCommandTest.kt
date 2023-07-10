package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.build.tools.lib.LibraryInstallException
import com.chromia.cli.it.TestDataCreator
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith


class InstallCommandTest {
    val path = "src/${InstallDirTarget.SOURCE.target}"
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)

    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        testDir = dir
        settingsFile = testDir.resolve("config.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        settings = parseModel(settingsFile)

    }

    @Test
    fun ridNotMatchingTest() {
        assertFailsWith<LibraryInstallException> {
            File(testDir.toFile(), "config.yml").writeText("""
            blockchains:
                hello:
                  module: main
            libs:
                fooFail:
                  registry: http://foo.com
                  path: lib
                  rid: x"11"
        """.trimIndent())
            InstallCommand { TestRepositoryCloner() }
                    .context { terminal = testTerminal }
                    .parse(listOf("-s", settingsFile.absolutePath, "-lib", "fooFail"))
        }
        assertThat(logger.output()).contains(
                "The rid for library fooFail does not match the configured value.\n" +
                        "Should be: 11\n" +
                        "Was: 1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5\n" +
                        "Do not blindly copy the calculated rid as the integrity of the library cannot be verified.")
    }

    @Test
    fun insecureTrueRidNotMatchingTest() {

        InstallCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "insecureBar"))

        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/insecureBar/d.rell").exists())
    }

    @Test
    fun alreadyPopulatedTargetTest() {
        TestRepositoryCloner().createFile(testDir.resolve("$path/foo").toFile(), "existingFileFoo.rell")
        TestRepositoryCloner().createFile(testDir.resolve("$path/bar").toFile(), "existingFileBar.rell")

        val res = InstallCommand { TestRepositoryCloner() }
                .test(listOf("-s", settingsFile.absolutePath, "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/existingFileBar.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/Foo/existingFileFoo.rell").exists())
        assertThat(res.output).contains("Library foo not up to date, reinstalling")
    }

    @Test
    fun singleLibraryTest() {

        InstallCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/foo/not/include/c.rell").exists())
    }


    @Test
    fun simpleSingleLibraryTest() {
        InstallCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "bar"))
        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
    }

    @Test
    fun simpleAllLibraryTest() {
        InstallCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath))
        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/insecureBar/d.rell").exists())
    }


    @Test
    fun multipleLibraryTest() {
        InstallCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "bar", "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/foo/not/include/c.rell").exists())
    }

    @Test
    fun missingSpecificLibraryTest(@TempDir dir: Path) {
        val res = InstallCommand { TestRepositoryCloner() }
                .test(listOf("-s", settingsFile.absolutePath, "-lib", "missingLib"))
        assertThat(res.stderr).contains("Error: invalid value for --library: Specified library(s) [missingLib] does not exist in config file")
    }


    @Test
    fun wrongRegistryTest(@TempDir dir: Path) {
        File(testDir.toFile(), "config.yml").writeText("""
            blockchains:
              hello:
                module: main
            libs:
                wrongRegistry:
                    registry: http://wrongAddress.com
                    path: lib
                    rid: x"13"
        """.trimIndent())

        val error = assertThrows<LibraryInstallException> {
            InstallCommand { TestRepositoryCloner() }
                    .test(listOf("-s", settingsFile.absolutePath, "-lib", "wrongRegistry"))
        }
        assertThat(error.message).isEqualTo("This is an error")
    }

    @Test
    fun nonRellFilesTest() {
        File(testDir.toFile(), "config.yml").writeText("""
            blockchains:
              hello:
                module: main
            libs:
                emptyRegistry:
                    registry: http://filter.com
                    path: lib
                    rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
        """.trimIndent())



        assertFailsWith<LibraryInstallException> {
            InstallCommand { TestRepositoryCloner() }
                    .context { terminal = testTerminal }
                    .parse(listOf("-s", settingsFile.absolutePath, "-lib", "emptyRegistry"))
        }
        assertThat(logger.output()).contains("Library emptyRegistry contains files that are not rell files.")
        assertThat(logger.output()).contains("/build/.lib/emptyRegistry/lib/a.yml")
        assertThat(logger.output()).contains("/build/.lib/emptyRegistry/lib/nested/b.yml")
    }

}