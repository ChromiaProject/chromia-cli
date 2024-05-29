package com.chromia.build.tools.lib

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.model.parseModel
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

internal class RellLibraryModelTest {
    @Test
    fun `can parse hex string of length 64 as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                            """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
    }

    @Test
    fun `can parse bytearray as brid`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments: 
                    foo:
                      url: "http://foo.com"
                      brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                     """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]?.blockchainRid).isEqualTo(BlockchainRid.buildFromHex("615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"))
    }

    @Test
    fun `can parse both formats for brid chains in deployed chains`(@TempDir dir: Path) {
        testData(dir) {
            config {
                deployments("""
                deployments:
                    foo:
                        url: "http://foo.com"
                        brid: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                        chains:
                            bc1: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5
                            bc2: x"615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5"
                """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.deployments["foo"]!!.chains.keys.size).isEqualTo(2)
    }

    @Test
    fun singleLibraryTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                lib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "2415A364EF7DB349F3AECE7201094D9294915641D35F7D1842E83DAAF9BBC558".hexStringToWrappedByteArray()))
                addFile("lib/foo/foo.rell", """module;""")
                addFile("lib/foo/bar.rell", """module; //Bar""")
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))

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
        testData(dir) {
            config {
                lib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "2415A364EF7DB349F3AECE7201094D9294915641D35F7D1842E83DAAF9BBC558".hexStringToWrappedByteArray()))
                lib("foo", RellLibraryModel("http://foo2.com", null, "lib", false, "615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5".hexStringToWrappedByteArray()))
            }
        }
        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        Assertions.assertEquals(settings.libs.size, 1)
        Assertions.assertEquals(settings.libs["foo"]!!.registry, "http://foo2.com")
    }

    @Test
    fun fullConfigParseTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                manualContent("""
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
            """.trimIndent()
                )
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(settings).isNotNull()
    }

    @Test
    fun multipleLibraryTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                lib("foo", RellLibraryModel("http://foo.com", null, "lib", false, "33B5C0C7909B01AD272346A49C4F4FCD6FF7E29685803F7F6C8B5EF320BF2F0C".hexStringToWrappedByteArray()))
                lib("bar", RellLibraryModel("http://bar.com", null, "lib", false, "E9A6EE3D187533034566A53007CA034F328CD469D986048B31EEE86E238E6E14".hexStringToWrappedByteArray()))
                addFile("lib/foo/main.rell", """module;""")
                addFile("lib/foo/api.rell", """module;""")
                addFile("lib/bar/main.rell", """module;""")
            }
        }
        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    @Test
    fun skipLibraryValidationTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                lib("foo", RellLibraryModel("http://foo.com", null, "lib", true, null))
                addFile("lib/foo/main.rell", """module;""")
                addFile("lib/foo/api.rell", """module;""")
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        val libraryVerifyer = LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib"))
        settings.libs.forEach {
            assertThat(libraryVerifyer.verifyLib(it.key, it.value)).isTrue()
        }
    }

    @Test
    fun `Can parse the real world examples of config files`() {
        val settingsFile = File(this.javaClass.classLoader.getResource("realWorldConfigs/d1.yml")!!.file)
        //TODO make more comprehensive tests
        parseModel(settingsFile)
    }

    @Test
    fun `Anchors and references resolves correct`(@TempDir dir: Path) {
        testData(dir) {
            config {
                definitions("""
                definitions: 
                  bar: &anc_bar
                    foo: hello
                """.trimIndent())
                blockchains("""
                blockchains:
                  bc1:
                    module: module1
                    moduleArgs: 
                      arg: *anc_bar 
                """.trimIndent())
            }
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]).isEqualTo(gtv("hello"))
    }

    @Test
    fun `list of byte array are validated and parsed`(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
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
        }

        val model = parseModel(dir.resolve("chromia.yml").toFile())
        val byteArrays = model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]
        assertThat(byteArrays?.get(0).toString()).isEqualTo("x\"1234\"")
        assertThat(byteArrays?.get(1).toString()).isEqualTo("x\"5678\"")
    }

    @Test
    fun `nested list of byte array are validated and parsed`(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
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
        }
        val model = parseModel(dir.resolve("chromia.yml").toFile())
        val byteArrays = model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]
        assertThat(byteArrays?.get(0)!![0].toString()).isEqualTo("x\"1234\"")
        assertThat(byteArrays[0][1].toString()).isEqualTo("x\"5678\"")
        assertThat(byteArrays[1].toString()).isEqualTo("x\"2468\"")
    }
}