package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.testData
import com.chromia.cli.model.parseModel
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class RellLibraryModelTest {
    private lateinit var settingsFile: File

    private fun createFile(dir: Path, name: String, code: String = ""): Path {
        return File(dir.toFile(), "$name.rell").apply {
            parentFile.mkdirs()
            writeText("""
                    module; $code
                """.trimIndent())
        }.toPath()
    }

    @Test
    fun `can parse hex string of length 64 as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                config {
                    blockchains("""
                blockchains:
                  a:
                    module: main
                    """.trimIndent()
                    )
                    deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                            """.trimIndent()
                    )
                }
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
    }

    @Test
    fun `can parse bytearray as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                config {
                    blockchains("""
                blockchains:
                  a:
                    module: main
                    """.trimIndent()
                    )
                    deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                            """.trimIndent()
                    )
                }
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
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
        createFile(dir, "lib/foo/foo")
        createFile(dir, "lib/foo/bar", "//Bar")
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("lib"))

        assertDoesNotThrow {
            libraryVerifyer.verifyLibs(settings.libs)
        }
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
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
                  quiet: true
                  
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
        val fileMap = mutableMapOf<String, List<Path>>()
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
        fileMap["foo"] = listOf(createFile(dir, "lib/foo/main"), createFile(dir, "lib/foo/api"))
        fileMap["bar"] = listOf(createFile(dir, "lib/bar/main"))
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    @Test
    fun skipLibraryValidationTest(@TempDir dir: Path) {
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
        createFile(dir, "lib/foo/main")
        createFile(dir, "lib/foo/api")

        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    @Test
    fun `Can parse the real world examples of config files`() {
        settingsFile = File(this.javaClass.classLoader.getResource("realWorldConfigs/d1.yml")!!.file)
        //TODO make more comprehensive tests
        parseModel(settingsFile)
    }

    @Test
    fun `Anchors and references resolves correct`(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                definitions: 
                  bar: &anc_bar
                    foo: hello
                blockchains:
                  bc1:
                    module: module1
                    moduleArgs: 
                      arg: *anc_bar
            """.trimIndent())
        }
        val model = parseModel(settingsFile)
        assertThat(model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]).isEqualTo(gtv("hello"))
    }

    @Test
    fun `list of byte array are validated and parsed`(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: module1
                    moduleArgs:
                        arg:
                            foo:
                                - x"1234"
                                - x"5678"              
            """.trimIndent())
        }
        val model = parseModel(settingsFile)
        val byteArrays = model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]
        assertThat(byteArrays?.get(0).toString()).isEqualTo("x\"1234\"")
        assertThat(byteArrays?.get(1).toString()).isEqualTo("x\"5678\"")
    }

    @Test
    fun `nested list of byte array are validated and parsed`(@TempDir dir: Path) {
        settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  bc1:
                    module: module1
                    moduleArgs:
                        arg:
                            foo:
                                -
                                    - x"1234"
                                    - x"5678"
                                - x"2468"
            """.trimIndent())
        }
        val model = parseModel(settingsFile)
        val byteArrays = model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]
        assertThat(byteArrays?.get(0)!![0].toString()).isEqualTo("x\"1234\"")
        assertThat(byteArrays[0][1].toString()).isEqualTo("x\"5678\"")
        assertThat(byteArrays[1].toString()).isEqualTo("x\"2468\"")
    }
}