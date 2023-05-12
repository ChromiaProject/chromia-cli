package com.chromia.cli

import com.chromia.cli.exception.LibraryNonSafeFiles
import com.chromia.cli.util.TestConsole
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith


class InstallCommandTest {
    lateinit var settings: File
    val path = "build/libs"

    lateinit var testConsole: TestConsole

    @BeforeEach
    fun setupTest() {
        testConsole = TestConsole()
    }

    @Test
    fun ridNotMatchingTest(@TempDir dir: Path) {
        val exception = assertFailsWith<CliktError> {
            settings = File(dir.toFile(), "config.yml").apply {
                writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      lib: lib
                      rid: x"11"  
            """.trimIndent())
            }

            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath))
        }
        assertEquals("The rid 11 for library foo does not match the calculated rid from the downloaded library, can not verify it has not be tampered with", exception.message)
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
                      lib: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"  
            """.trimIndent())
        }

        TestRepositoryCloner().createFile(dir.resolve("$path/foo/existingFileFoo.rell").toFile(), "existingFileFoo.rell")
        TestRepositoryCloner().createFile(dir.resolve("$path/bar/existingFileBar.rell").toFile(), "existingFileFoo.rell")

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "$path/bar/existingFileBar.rell").exists())
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
                      lib: lib
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
                      lib: lib
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
                      lib: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      lib: lib
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
    fun wrongRegistryTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://wrongAddress.com
                      lib: lib
                      rid: x"13"
            """.trimIndent())
        }
        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))

        Assertions.assertEquals("Invalid remote host. Error: This is an error\n", testConsole.out[0].first)
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
                      lib: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
            """.trimIndent())
        }
        val exception = assertFailsWith<LibraryNonSafeFiles> {
            InstallCommand { TestRepositoryCloner() }
                    .context { console = testConsole }
                    .parse(listOf("-s", settings.absolutePath))
        }
        assertEquals("The library lib contains files that has non rell type files. Can not verify it has not be tampered with", exception.message)
    }


}