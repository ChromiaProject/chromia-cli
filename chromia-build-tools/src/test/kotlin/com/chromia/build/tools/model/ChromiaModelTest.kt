package com.chromia.build.tools.model

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.testData
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.parseModel
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvBigInteger
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvNull
import net.postchain.gtv.GtvString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir

internal class ChromiaModelTest {

    @Test
    fun `Can parse the real world examples of config files`() {
        val settingsFile = File(this.javaClass.classLoader.getResource("realWorldConfigs/d1.yml")!!.file)
        //TODO make more comprehensive tests
        parseModel(settingsFile)
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
        assertThat(model.blockchains["bc1"]!!.moduleArgs["arg"]!!["foo"]).isEqualTo(GtvFactory.gtv("hello"))
    }

    @Test
    fun fullConfigParseTest(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
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
                """.trimIndent())
                deployments("""
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
                """.trimIndent())
                compile("""
                compile:
                  rellVersion: 0.13.5
                  source: src
                  target: build
                  deprecatedError: false
                  quiet: true
                """.trimIndent())
                database("""
                database:
                    password: postchain
                    username: postchain
                    database: postchain
                    host: localhost
                    logSqlErrors: true
                    schema: rell_app
                    driver: org.postgresql.Driver                    
                """.trimIndent())
                test("""
                test:
                    modules:
                        - test.bar
                        - test.foo
                    moduleArgs:
                        moduleOne:
                            hex: x"1234"
                            string: foo
                    failOnError: true   
                """.trimIndent())
                setFullLib("""
                libs:
                    lib:
                        registry: https://bar.com
                        path: path/foo
                        tagOrBranch: branchOne
                        rid: x"1234"
                        insecure: false      
                """.trimIndent())
            }
        }

        val settings = parseModel(dir.resolve("chromia.yml").toFile())
        assertThat(settings).isNotNull()
    }

    @Test
    fun `moduleArgs supports all primitive GTV types`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                  my-rell-dapp:
                    module: main
                    moduleArgs:
                      main:
                        s: abc
                        i: 17
                        ba: x"abc123"
                        t: true
                        f: false
                        n: null
                        sl: "1234L"
                        bi: 1234L                        
            """.trimIndent())
        }

        val settings = parseModel(settingsFile)
        val moduleArgs = settings.blockchains["my-rell-dapp"]!!.moduleArgs["main"]!!
        assertThat(moduleArgs["s"]).isEqualTo(GtvString("abc"))
        assertThat(moduleArgs["i"]).isEqualTo(GtvInteger(17L))
        assertThat(moduleArgs["ba"]).isEqualTo(GtvByteArray("abc123".hexStringToByteArray()))
        assertThat(moduleArgs["t"]).isEqualTo(GtvInteger(1))
        assertThat(moduleArgs["f"]).isEqualTo(GtvInteger(0))
        assertThat(moduleArgs["n"]).isEqualTo(GtvNull)
        assertThat(moduleArgs["sl"]).isEqualTo(GtvString("1234L"))
        assertThat(moduleArgs["bi"]).isEqualTo(GtvBigInteger(BigInteger("1234")))
    }

    @Test
    fun `blockchains map is ordered`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                     chainB:
                         module: first
                     chainA:
                         module: second
            """.trimIndent())
        }

        val settings = parseModel(settingsFile)
        assertThat(settings.blockchains.toList().map { it.first }).isEqualTo(listOf("chainB", "chainA"))
    }

    @Test
    fun `library is parsed`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                     my_lib:
                         module: first
                         type: library
            """.trimIndent())
        }

        val settings = parseModel(settingsFile)
        assertThat(settings.blockchains.keys).containsOnly("my_lib")
        assertThat(settings.blockchains.values.single().type).isEqualTo(BlockchainModel.Type.LIBRARY)
    }

    @Test
    fun `Wrong type is rejected`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                     my_lib:
                         module: main
                         type: no_type
            """.trimIndent())
        }

        assertThrows<ValidationException> {
            parseModel(settingsFile)
        }
    }

    @Test
    fun `docs title is required`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                     chainB:
                         module: first
                docs:
                    not_correct_property: my_faulty_config
            """.trimIndent())
        }
        val res = assertThrows<ValidationException> { parseModel(settingsFile) }

        assertThat(res.message!!).all {
            contains("Additional property 'not_correct_property' found but was invalid")
            contains("Required property \"title\" not found")
        }
    }

    @Test
    fun `docs properties validates type of input`(@TempDir dir: Path) {
        val settingsFile = File(dir.toFile(), "chromia.yml").apply {
            writeText("""
                blockchains:
                     chainB:
                         module: first
                docs:
                    title: 2
                    customStyleSheets:
                      - 2
                    customAssets:
                      - 2
                    additionalContent:
                      - 2
                    footerMessage: 2
            """.trimIndent())
        }
        val res = assertThrows<ValidationException> { parseModel(settingsFile) }

        assertThat(res.message!!).all {
            contains("Incorrect type, expected string (location: docs->title)")
            contains("Incorrect type, expected string (location: docs->customStyleSheets->0)")
            contains("Incorrect type, expected string (location: docs->customAssets->0)")
            contains("Incorrect type, expected string (location: docs->additionalContent->0)")
            contains("Incorrect type, expected string (location: docs->footerMessage)")
        }
    }
}
