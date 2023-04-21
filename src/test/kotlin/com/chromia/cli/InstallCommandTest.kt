package com.chromia.cli

import com.chromia.cli.util.RepositoryCloner
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class InstallCommandTest {
    lateinit var settings: File

    lateinit var testConsole: TestConsole

    @BeforeEach
    fun setupTest() {
        testConsole = TestConsole()
    }

    @Test
    fun singleDependencyTest(@TempDir dir: Path) {

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
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/foo/nested/b.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "build/libs/foo/not/include/c.rell").exists())
    }

    @Test
    fun multipleDependencyTest(@TempDir dir: Path) {

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
                    bar:
                      registry: http://bar.com
                      lib: lib
                      rid: x"12" 
            """.trimIndent())
        }

        InstallCommand { TestRepositoryCloner() }
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/foo/a.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/foo/nested/b.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/bar/d.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "build/libs/foo/not/include/c.rell").exists())
    }

    @Test
    fun duplicateDependencyTest(@TempDir dir: Path) {

        settings = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                  bc2:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      lib: lib
                      rid: x"11"
                    foo:
                      registry: http://foo.com
                      lib: lib
                      rid: x"11"
            """.trimIndent())
        }

    }

    @Test
    fun wrongRegistryDependencyTest(@TempDir dir: Path) {

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

    class TestRepositoryCloner : RepositoryCloner {
        override fun clone(registry: String, target: File) {
            when (registry) {
                "http://bar.com" -> createFile(target, "lib/d")
                "http://foo.com" -> {
                    createFile(target, "lib/a")
                    createFile(target, "lib/nested/b")
                    createFile(target, "not/include/c")
                }

                "http://wrongAddress.com" -> throw InvalidRemoteException("This is an error")
            }
        }

        private fun createFile(dir: File, name: String) {
            with(File(dir, "$name.rell")) {
                parentFile.mkdirs()
                writeText("""
                    module; 
                """.trimIndent())
            }
        }
    }
}