package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.cli.model.parseModel
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

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
    fun fullConfigParseTest(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                    foo:
                        module: main
                        config:
                            height: 1
                        moduleArgs:
                            moduleOne:
                                hex: x"1234"
                                string: foo
                            moduleTwo:
                                hex: x"5678"
                                string: bar
                        test:
                            modules:
                                - test.bar
                                - test.foo
                            moduleArgs:
                                moduleOne:
                                    hex: x"1234"
                                    string: foobar
                
                deployments:
                  testnet:
                    brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                    url:
                      - https://foo.com
                      - https://bar.com
                    container: 1234id
                    chains:
                      foo: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5" 
                      
                  mainnet:
                    brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                    url: https://bar.com
                    container: 1234id
                
                compile:
                  rellVersion: 0.13.5
                  source: src
                  target: build
                  deprecatedError: false
                  quite: true
                  
                database:
                    password: postchain
                    username: postchain
                    database: postchain
                    host: localhost
                    logSqlErrors: true
                    schema: rell_app
                    driver: org.postgresql.Driver
                
                test:
                    modules:
                        - test.bar
                        - test.foo
                    moduleArgs:
                        moduleOne:
                            hex: x"1234"
                            string: foo
                    failOnError: true
                libs:
                    lib:
                        registry: https://bar.com
                        path: path/foo
                        tagOrBranch: branchOne
                        rid: x"1234"
                        insecure: false
            """.trimIndent())
        }

        val settings = parseModel(settingsFile)

        assertThat(settings).isNotNull()
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