package com.chromia.cli

import com.chromia.cli.model.parseModel
import com.chromia.cli.util.DependencyResolver
import com.chromia.cli.util.RegisteredLib
import com.chromia.cli.util.RepositoryCloner
import com.chromia.cli.util.Settings
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import net.postchain.common.types.WrappedByteArray
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

        InstallCommand({ TestRepositoryCloner() }, { TestDependencyResolver("testLib", rid = WrappedByteArray.fromHex("")) })
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/testLib-foo.com/main.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/testLib-foo.com/nested/main.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "build/libs/testLib-foo.com/not/include/main.rell").exists())
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

        InstallCommand({ TestRepositoryCloner() }, { TestDependencyResolver("testLib", rid = WrappedByteArray.fromHex("")) })
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))
        Assertions.assertTrue(File(dir.toFile(), "config.yml").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/testLib-foo.com/main.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/testLib-foo.com/nested/main.rell").exists())
        Assertions.assertTrue(File(dir.toFile(), "build/libs/testLib-bar.com/main.rell").exists())
        Assertions.assertFalse(File(dir.toFile(), "build/libs/testLib-foo.com/not/include/main.rell").exists())
    }

    @Test
    fun duplicateDependencyTest(@TempDir dir: Path) {

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
                  bc2:
                    module: main
                    libs:
                        foo:
                          registry: http://foo.com
                          lib: lib
                          rid: x"11"
            """.trimIndent())
        }
        val resolver = TestDependencyResolver("testLib", rid = WrappedByteArray.fromHex(""))
        Assertions.assertEquals(resolver.getDependencies(Settings(settings, parseModel(settings))).size, 1)
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
        InstallCommand({ TestRepositoryCloner() }, { TestDependencyResolver("testLib", rid = WrappedByteArray.fromHex("")) })
                .context { console = testConsole }
                .parse(listOf("-s", settings.absolutePath))

        Assertions.assertEquals("Invalid remote host. Error: This is an error\n", testConsole.out[0].first)
    }

    class TestRepositoryCloner : RepositoryCloner {
        override fun clone(registry: String, dir: File) {
            when (registry) {
                "http://bar.com" -> createFile(dir)
                "http://foo.com" -> {
                    createFile(dir)
                    createNestedFile(dir)
                    createFileToNotInclude(dir)
                }

                "http://wrongAddress.com" -> throw InvalidRemoteException("This is an error")
            }
        }

        private fun createFile(dir: File) {
            with(File(dir, "lib/main.rell")) {
                parentFile.mkdirs()
                writeText("""
                    module; 
                """.trimIndent())
            }
        }

        private fun createNestedFile(dir: File) {
            with(File(dir, "lib/nested/main.rell")) {
                parentFile.mkdirs()
                writeText("""
                    module; 
                """.trimIndent())
            }
        }

        private fun createFileToNotInclude(dir: File) {
            with(File(dir, "not/include/main.rell")) {
                parentFile.mkdirs()
                writeText("""
                    module; 
                """.trimIndent())
            }
        }
    }

    class TestDependencyResolver(private val name: String, val rid: WrappedByteArray) : DependencyResolver {
        override fun checkHash(rid: WrappedByteArray): Boolean {
            TODO("Not yet implemented")
        }

        override fun getLib(registry: String): RegisteredLib {
            return RegisteredLib("$name-${registry.replace(Regex("http(s)?://|www\\.|/.*"), "")}", rid)
        }

    }
}