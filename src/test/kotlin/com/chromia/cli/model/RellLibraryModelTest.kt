package com.chromia.cli.model

import com.chromia.cli.util.Settings
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
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
                      lib: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"  
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"))

        settings.libs.forEach {
            Assertions.assertTrue(it.value.validateRid(fileMap[it.key]!!))
        }
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
                      lib: lib
                      rid: x"1FA06E7C18BE7AE88C782DDCD9FD4FD16CEBA7C5E2ABA72419413F73975185A5"
                    bar:
                      registry: http://bar.com
                      lib: lib
                      rid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"  
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))
        fileMap["bar"] = listOf(createFile(dir.toFile(), "lib/bar/main"))

        settings.libs.forEach {
            Assertions.assertTrue(it.value.validateRid(fileMap[it.key]!!))
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
                      lib: lib
                      verifyRid: false
            """.trimIndent())
        }

        val settings = Settings(settingsFile, parseModel(settingsFile))
        fileMap["foo"] = listOf(createFile(dir.toFile(), "lib/foo/main"), createFile(dir.toFile(), "lib/foo/api"))

        settings.libs.forEach {
            Assertions.assertTrue(it.value.validateRid(fileMap[it.key]!!))
        }
    }
}