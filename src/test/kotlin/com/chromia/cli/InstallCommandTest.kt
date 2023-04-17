package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.eclipse.jgit.api.errors.InvalidRemoteException
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


class InstallCommandTest {

    companion object {
        lateinit var settings: File

        @BeforeAll
        @JvmStatic
        fun setup(@TempDir dir: Path) {
            with(File(dir.toFile(), "src/main.rell")) {
                parentFile.mkdirs()
                writeText("""
                module;
            """.trimIndent())
            }
            settings = File(dir.toFile(), "config.yml").apply {
                writeText("""
                blockchains:
                  bc1:
                    module: main
                    libs:
                        foo:
                          registry: http://foo.com
                          lib: lib
                          rid: x"987654"
                        bar:
                          registry: http://bar.com
                          lib: lib/
                          rid: x"58302025"
                  bc2:
                    module: main
                    libs:
                        wrong:
                          registry: http://wrongAddress.com
                          lib: client/lib/ft3/
                          rid: x"123456"
                        bar:
                          registry: http://bar.com
                          lib: lib/
                          rid: x"58302025"
  
            """.trimIndent())
            }
        }
    }


    lateinit var testConsole: TestConsole

    @BeforeEach
    fun setupTest() {
        testConsole = TestConsole()
    }

    @Test
    fun tes(@TempDir dir: Path) {
        InstallCommand { TestRepositoryCloner() }.context { console = testConsole }.parse(listOf("-s", settings.absolutePath, "--target", "/Users/carljernbacker/Desktop/chromaway/chromia-cli/ex/build/"))
        //TODO test that the correct number of files are created in temp dir
        //TODO the flatmap works and duplicate libs are removed
        //TODO make sure that errors are caught and displayed in a nice way

    }

    class TestRepositoryCloner : RepositoryCloner {
        override fun clone(registry: String, dir: File) {
            when (registry) {
                "http://bar.com" -> createFile(dir)
                "http://foo.com" -> createFile(dir)
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
    }
}