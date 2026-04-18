package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.chromia.build.tools.testData
import com.chromia.cli.command.library.management.InstallLibraryCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.hexStringToWrappedByteArray
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.test.Ignore

@Disabled("Temporarily disabled")
class InstallLibraryCommandTest {
    val path = "src/lib"
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File

    @BeforeEach
    fun setup() {
        testData(testDir) {
            config {
                addLib("foo", TestRepositoryCloner.foo.model)
                addLib("bar", TestRepositoryCloner.bar.model)
                addLib("insecureBar", TestRepositoryCloner.bar.model.copy(insecure = true, rid = "11".hexStringToWrappedByteArray()))
            }
        }

        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        settings = parseModel(settingsFile)
    }

    @Test
    fun ridNotMatchingTest() {
        File(testDir.toFile(), "chromia.yml").writeText("""
        blockchains:
            my_rell_dapp:
              module: main
        libs:
            fooFail:
              registry: http://foo.com
              path: path/to/foo
              rid: x"11"
    """.trimIndent())
        assertThrows<CliktError> {
            InstallLibraryCommand { TestRepositoryCloner() }
                .context { terminal = testTerminal }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "fooFail"))
        }
        assertThat(logger.output()).contains("Was: 046AC0AE25375C1CF7A819D0649F6373A49B53265937D6E9D6CC6CE4317B0EB1")
    }

    @Test
    fun insecureTrueRidNotMatchingTest() {
        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "insecureBar"))

        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/insecureBar/d.rell").exists())
    }

    @Test
    fun alreadyPopulatedTargetTest() {
        val testRepositoryCloner = TestRepositoryCloner()
        testRepositoryCloner.createFile(testDir.resolve("$path/foo"), Path("existingFileFoo.rell"))
        testRepositoryCloner.createFile(testDir.resolve("$path/bar"), Path("existingFileBar.rell"))

        val res = InstallLibraryCommand { testRepositoryCloner }
                .test(listOf("-s", settingsFile.absolutePath, "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/existingFileBar.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/Foo/existingFileFoo.rell").exists())
        assertThat(res.output).contains("Library foo not up to date, reinstalling")
    }

    @Test
    fun aliasImportIsAllowed() {
        testData(testDir) {
            config {
                addLib("alias_foo", TestRepositoryCloner.foo.model)
            }
        }
        assertDoesNotThrow {
            InstallLibraryCommand { TestRepositoryCloner() }
                    .parse(listOf("-s", settingsFile.absolutePath, "-lib", "alias_foo"))
        }
    }

    @Test
    fun singleLibraryTest() {

        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/foo/not/include/c.rell").exists())
    }


    @Test
    fun simpleSingleLibraryTest() {
        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "bar"))
        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
    }

    @Test
    fun simpleAllLibraryTest() {
        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath))
        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/insecureBar/d.rell").exists())
    }


    @Test
    fun multipleLibraryTest() {
        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "bar", "-lib", "foo"))
        Assertions.assertTrue(File(testDir.toFile(), "chromia.yml").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertTrue(File(testDir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertFalse(File(testDir.toFile(), "$path/foo/not/include/c.rell").exists())
    }

    @Test
    fun missingSpecificLibraryTest() {
        val res = InstallLibraryCommand { TestRepositoryCloner() }
                .test(listOf("-s", settingsFile.absolutePath, "-lib", "missingLib"))
        assertThat(res.stderr).contains("invalid value for --library: Specified library(ies) [missingLib] not found in ${settingsFile.absolutePath}")
    }


    @Test
    fun wrongRegistryTest() {
        testData(testDir) {
            config {
                addLib("wrongRegistry", RellLibraryModel("http://wrongAddress.com", path = "lib", rid = null))
            }
        }

        val output = InstallLibraryCommand { TestRepositoryCloner() }
                    .test(listOf("-s", settingsFile.absolutePath, "-lib", "wrongRegistry"))
        assertThat(output.stderr).contains("Repository does not exist")
    }

    @Test
    fun nonRellFilesTest() {
        testData(testDir) {
            config {
                addLib("emptyRegistry", TestRepositoryCloner.filter.model)
            }
        }

        assertDoesNotThrow {
            InstallLibraryCommand { TestRepositoryCloner() }
                    .context { terminal = testTerminal }
                    .parse(listOf("-s", settingsFile.absolutePath, "-lib", "emptyRegistry"))
        }
        val (rellFiles, nonRellFiles) = Files.walk(testDir.resolve("src/lib/emptyRegistry"))
                .filter { !it.isDirectory() }.toList().partition { it.extension == "rell" }
        assertThat(nonRellFiles).isEmpty()
        assertThat(rellFiles.size).isEqualTo(2)
    }

    @Test
    fun forceFlagWithRidMismatchTest() {
        File(testDir.toFile(), "chromia.yml").writeText("""
        blockchains:
          my_rell_dapp:
            module: main
        libs:
          foo:
            registry: http://foo.com
            path: path/to/foo
            rid: x"046AC0AE25375C1CF7A819D0649F6373A49B53265937D6E9D6CC6CE4317B0EB2"
    """.trimIndent())
        // With --force flag, installation should succeed despite RID mismatch
        InstallLibraryCommand { TestRepositoryCloner() }
                .parse(listOf("-s", settingsFile.absolutePath, "-lib", "foo", "--force"))
        Assertions.assertTrue(File(testDir.toFile(), "$path/foo/a.rell").exists())
    }

    @Test
    fun emptyLibsSectionTest() {
        File(testDir.toFile(), "chromia.yml").writeText("""
        blockchains:
            my_rell_dapp:
              module: main
    """.trimIndent())
            InstallLibraryCommand { TestRepositoryCloner() }
                .context { terminal = testTerminal }
                .parse(listOf("-s", settingsFile.absolutePath))

        assertThat(logger.output()).contains("No libraries found in: ${settingsFile.absolutePath}")
    }

    @Test
    fun missingModelFileTest() {
        val nonExistentFile = testDir.resolve("nonexistent.yml").toFile()
        val res = InstallLibraryCommand { TestRepositoryCloner() }
                .test(listOf("-s", nonExistentFile.absolutePath))
        assertThat(res.stderr).contains("file \"${nonExistentFile.absolutePath}\" does not exist")
    }

    @Test
    fun multipleMissingLibrariesTest() {
        val res = InstallLibraryCommand { TestRepositoryCloner() }
                .test(listOf("-s", settingsFile.absolutePath, "-lib", "missingLib1", "-lib", "missingLib2"))
        assertThat(res.stderr).contains("invalid value for --library")
        assertThat(res.stderr).contains("missingLib1")
        assertThat(res.stderr).contains("missingLib2")
    }
}