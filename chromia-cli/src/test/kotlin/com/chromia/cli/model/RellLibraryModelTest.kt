package com.chromia.cli.model

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.cli.util.Settings
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class RellLibraryModelTest {
    private lateinit var settingsFile: File
    private var fileMap = mutableMapOf<String, List<File>>()

    private fun createFile(dir: File, name: String): File {
        return File(dir, "$name.rell").apply {
            parentFile.mkdirs()
            writeText("""
                    module; 
                """.trimIndent())
        }
    }

    @AfterEach
    fun setup() {
        fileMap.clear()
    }

    @Test
    fun singleLibraryTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"  
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"))
        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv() {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })

        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!))
        }
    }

    @Test
    fun unrecognizedFieldInRellLibraryModelTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                      some_unexpected_field: 123 
            """.trimIndent())
        }
        val throwable = assertThrows<UnrecognizedPropertyException> {
            Settings(settingsFile, parseModel(settingsFile))
        }
        assertThat(throwable.message!!).contains("Unrecognized field \"some_unexpected_field\"")
    }

    //TODO this is not what we want, want the parser to throw error if duplicate keys "name" of libs
    @Test
    fun conflictingLibraryNameIsOverriddenTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo1.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                    foo:
                      registry: http://foo2.com
                      path: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"  
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        Assertions.assertEquals(settings.libs.size, 1)
        Assertions.assertEquals(settings.libs["foo"]!!.registry, "http://foo2.com")
    }


    @Test
    fun multipleLibraryTest(@TempDir dir: Path) {
        val fileMap = mutableMapOf<String, List<File>>()
        settingsFile = File(dir.toFile(), "config.yml").apply {
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

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))
        fileMap["bar"] = listOf(createFile(dir.toFile(), "lib/bar/main"))
        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv() {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!))
        }
    }

    @Test
    fun skipLibraryValidationTest(@TempDir dir: Path) {
        val fileMap = mutableMapOf<String, List<File>>()
        settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      insecure: true
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))

        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv() {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!))
        }
    }
}