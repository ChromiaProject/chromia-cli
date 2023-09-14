package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isTrue
import com.chromia.cli.model.parseModel
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import java.io.File
import java.nio.file.Path
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

internal class RellLibraryModelTest {
    private lateinit var settingsFile: File
    private var fileMap = mutableMapOf<String, List<File>>()

    private fun createFile(dir: File, name: String, code: String = ""): File {
        return File(dir, "$name.rell").apply {
            parentFile.mkdirs()
            writeText("""
                    module; $code
                """.trimIndent())
        }
    }

    @AfterEach
    fun setup() {
        fileMap.clear()
    }

    @Test
    fun singleLibraryTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: main
                libs:
                    foo:
                      registry: http://foo.com
                      path: lib
                      rid: x"B06598BD8F8963AAE587ECBE3CA5442DE437F332D60DFC1FF10D77E4D524A6B5"  
            """.trimIndent())
        }

        val settings = parseModel(settingsFile)
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo"), createFile(dir.toFile(), "lib/bar", "//Bar"))
        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })

        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!)).isTrue()
        }
    }

    @Test
    fun unrecognizedFieldInRellLibraryModelTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
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
        val throwable = assertThrows<UnrecognizedPropertyException> { parseModel(settingsFile) }
        assertThat(throwable.message!!).contains("Unrecognized field \"some_unexpected_field\"")
    }

    //TODO this is not what we want, want the parser to throw error if duplicate keys "name" of libs
    @Test
    fun conflictingLibraryNameIsOverriddenTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
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

        val settings = parseModel(settingsFile)
        Assertions.assertEquals(settings.libs.size, 1)
        Assertions.assertEquals(settings.libs["foo"]!!.registry, "http://foo2.com")
    }


    @Test
    fun multipleLibraryTest(@TempDir dir: Path) {
        val fileMap = mutableMapOf<String, List<File>>()
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
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

        val settings = parseModel(settingsFile)
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))
        fileMap["bar"] = listOf(createFile(dir.toFile(), "lib/bar/main"))
        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!)).isTrue()
        }
    }

    @Test
    fun skipLibraryValidationTest(@TempDir dir: Path) {
        val fileMap = mutableMapOf<String, List<File>>()
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
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

        val settings = parseModel(settingsFile)
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))

        val libraryVerifyer = LibraryVerifyer(object : RellCliEnv {
            override fun error(msg: String) = println(msg)
            override fun print(msg: String) = println(msg)
        })
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.value, it.key, fileMap[it.key]!!)).isTrue()
        }
    }
}