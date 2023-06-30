package com.chromia.cli

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.build.tools.lib.LibraryInstallException
import com.chromia.cli.util.TestConsole
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith


class InstallCommandTest {
    lateinit var settings: File
    val path = "src/${InstallDirTarget.SOURCE.target}"

    lateinit var testConsole: TestConsole

    @BeforeEach
    fun setupTest() {
        testConsole = TestConsole()
    }

    @Test
    fun ridNotMatchingTest(@TempDir dir: Path) {
        val exception = assertFailsWith<LibraryInstallException> {
            settings = File(dir.toFile(), "config.yml").apply {
                writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"11"  
            """.trimIndent())
            }

            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath))
        }
        testConsole.assertContains(
        "The rid for library foo does not match the configured value.\n" +
                "Should be: 11\n" +
                "Was: 1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5\n" +
                "Do not blindly copy the calculated rid as the integrity of the library cannot be verified.")
    }

    @Test
    fun insecureTrueRidNotMatchingTest(@TempDir dir: Path) {
        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"11"
                      insecure: true
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))

        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/d.rell").exists())
    }

    @Test
    fun alreadyPopulatedTargetTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"  
            """.trimIndent())
        }

        TestRepositoryCloner().createFile(dir.resolve("$path/foo").toFile(), "existingFileFoo.rell")
        TestRepositoryCloner().createFile(dir.resolve("$path/bar").toFile(), "existingFileBar.rell")

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/existingFileBar.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/Foo/existingFileFoo.rell").exists())
    }

    @Test
    fun singleLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"  
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/not/include/c.rell").exists())
    }


    @Test
    fun simpleSingleLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/d.rell").exists())
    }


    @Test
    fun multipleLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/nested/b.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/not/include/c.rell").exists())
    }

    @Test
    fun missingSpecificLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
            """.trimIndent())
        }

        assertFailsWith<BadParameterValue> {
            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath, "-lib", "bar2"))
        }
    }

    @Test
    fun specificLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath, "-lib", "bar"))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/nested/b.rell").exists())
    }

    @Test
    fun multipleSpecificLibraryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                    bar2:
                      registry: http://bar.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath, "-lib", "bar", "-lib", "bar2"))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/d.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar2/d.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "$path/foo/nested/b.rell").exists())
    }

    @Test
    fun wrongRegistryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://wrongAddress.com
                      path: lib
                      rid: x"13"
            """.trimIndent())
        }
        val error = assertThrows<LibraryInstallException> {
            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath))
        }

        assertThat(error.message).isEqualTo("This is an error")
    }

    @Test
    fun nonRellFilesTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://filter.com
                      path: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
            """.trimIndent())
        }
        val exception = assertFailsWith<LibraryInstallException> {
            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath))
        }
        testConsole.assertContains("Library foo contains files that are not rell files.")
        testConsole.assertContains("/build/.lib/foo/lib/a.yml")
        testConsole.assertContains("/build/.lib/foo/lib/nested/b.yml")
    }

}