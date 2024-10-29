package com.chromia.api

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.model.parseModel
import net.postchain.common.exception.UserMistake
import java.nio.file.Path
import net.postchain.common.hexStringToByteArray
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.api.base.RellCliExitException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertFailsWith

internal class ChromiaCompileApiTest {
    private val cliEnv = RellCliEnv.NULL
    @TempDir
    lateinit var dir: Path

    @Test
    fun `Compile Simple App`(@TempDir dir: Path) {
        testData(dir)
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().name).isEqualTo("hello")
        assertThat(result.first().config["gtx"]?.asDict()?.get("rell")?.asDict()?.containsKey("modules")).isEqualTo(true)
    }

    @Test
    fun `Compile simple library`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module;""")
            addSourceFile("lib/my/test/module.rell", """@test module;""")
        }

        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        val rid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src/lib/my"), RidStrategy.LIST)
        assertThat(result.size).isEqualTo(1)
        assertThat(result.first().name).isEqualTo("my")
        assertThat(result.first().config["gtx"]?.get("rell")?.get("version")).isNotNull()
        val computedRid = result.first().config["rid"]
        assertThat(computedRid).isEqualTo(gtv(rid))
        assertThat(computedRid).isEqualTo(gtv("E3E0DECFDD9E90017933CA3A51DEF38DFE8D0D887441859015C756C3BA82CDED".hexStringToByteArray()))
        val model = RellLibraryModel("", path = "", rid = WrappedByteArray(computedRid!!.asByteArray()))
        LibraryVerifyer(RellCliEnv.DEFAULT, dir.resolve("src/lib")).verifyLib("my", model)
    }

    @Test
    fun `Library that depends on lib with missing 3rd party lib should compile`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module; import lib.other;""")
            addSourceFile("lib/other/module.rell", """module;""")
            addSourceFile("lib/other/broken.rell", """module; import lib.nonexistent;""") // This file is not imported from lib.my
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val rid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src/lib/my"), RidStrategy.LIST)
        assertThat(result.first().config["rid"]?.asByteArray()?.wrap()).isEqualTo(rid)
    }

    @Test
    fun `Library with mismatching name throws`() {
        val blockchainName = "wrong_name"
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  $blockchainName:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module;""")
        }
        val err = assertThrows<UserMistake> {
            ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        }
        assertThat(err.message).isEqualTo("Library $blockchainName not found. Please verify that the name of the library matches the folder name in lib folder.")
    }

    @Test
    fun `Library with broken rell code is caught`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/other.rell", """module; function f() =;""")
            addSourceFile("lib/my/module.rell", """module;""") // Note that *other* is not imported
        }
        val err = assertThrows<RellCliExitException> {
            ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        }
        assertThat(err.message).isEqualTo("Compilation failed")
    }

    @Test
    fun `Library with missing import in side-module is not considered an error`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/other.rell", """module; import lib.missing;""")
            addSourceFile("lib/my/module.rell", """module;""") // Note that *other* is not imported
        }
        assertDoesNotThrow {
            ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        }
    }

    @Test
    fun `Library with exposed test module is included`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                    test:
                      modules:
                        - lib.my.test.utils
                        - tests
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module;""")
            addSourceFile("lib/my/inner/module.rell", """module;""")
            addSourceFile("lib/my/test/utils.rell", """@test module;""")
            addSourceFile("tests/lib_my_test.rell", """@test module; import lib.my.test.utils; function test() {}""")
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val rid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src/lib/my"), RidStrategy.LIST)
        assertThat(result.first().config["rid"]?.asByteArray()?.wrap()).isEqualTo(rid)
        val allFilesRid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src"), RidStrategy.LIST)
        assertThat(allFilesRid).isNotEqualTo(rid)
    }

    @Test
    fun `Library exposing module arguments should compile anyway`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module; struct module_args { name; }""")
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
    }

    @Test
    fun `verify library`(@TempDir dir: Path) {
        testData(dir)
        val result = ChromiaCompileApi.verify(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result).isTrue()
    }

    @Test
    fun `Rell and static web content`() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        webStatic: web
                        webCacheTtlSeconds: 60                    
                """.trimIndent())
            }
            addFile("web/index.html", "<html></html>")
            addFile("web/img/image.png", byteArrayOf(1, 2, 3, 4))
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val outputGtv = result[0].config

        assertThat(outputGtv["gtx"]?.get("modules")?.asArray()?.map { it.asString() }!!).containsExactlyInAnyOrder(
                "net.postchain.rell.module.RellPostchainModuleFactory",
                "net.postchain.web.WebStaticGTXModuleFactory",
                "net.postchain.gtx.StandardOpsGTXModule",
        )

        assertThat(outputGtv["gtx"]?.get("rell")?.get("modules")?.asArray()?.get(0)?.asString()).isEqualTo("main")

        assertThat(outputGtv["web_static"]?.get("cache_ttl_seconds")?.asInteger()).isEqualTo(60L)
        assertThat(outputGtv["web_static"]?.get("content")?.get("index.html")?.get("content")?.asString()).isEqualTo("<html></html>")
        assertThat(outputGtv["web_static"]?.get("content")?.get("index.html")?.get("content_type")?.asString()).isEqualTo("text/html")
        assertThat(outputGtv["web_static"]?.get("content")?.get("img/image.png")?.get("content")?.asByteArray()?.wrap()).isEqualTo(byteArrayOf(1, 2, 3, 4).wrap())
        assertThat(outputGtv["web_static"]?.get("content")?.get("img/image.png")?.get("content_type")?.asString()).isEqualTo("image/png")
    }

    @Test
    fun `Only static web content`() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        webStatic: web
                        webCacheTtlSeconds: 60                    
                """.trimIndent())
            }
            addFile("web/index.html", "<html></html>")
            addFile("web/img/image.png", byteArrayOf(1, 2, 3, 4))
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val outputGtv = result[0].config
        assertThat(outputGtv["gtx"]?.get("modules")?.asArray()?.map { it.asString() }!!).containsExactlyInAnyOrder(
                "net.postchain.web.WebStaticGTXModuleFactory",
                "net.postchain.gtx.StandardOpsGTXModule",
        )

        assertThat(outputGtv["gtx"]?.get("rell")).isNull()

        assertThat(outputGtv["web_static"]?.get("cache_ttl_seconds")?.asInteger()).isEqualTo(60L)
        assertThat(outputGtv["web_static"]?.get("content")?.get("index.html")?.get("content")?.asString()).isEqualTo("<html></html>")
        assertThat(outputGtv["web_static"]?.get("content")?.get("index.html")?.get("content_type")?.asString()).isEqualTo("text/html")
        assertThat(outputGtv["web_static"]?.get("content")?.get("img/image.png")?.get("content")?.asByteArray()?.wrap()).isEqualTo(byteArrayOf(1, 2, 3, 4).wrap())
        assertThat(outputGtv["web_static"]?.get("content")?.get("img/image.png")?.get("content_type")?.asString()).isEqualTo("image/png")
    }

    @Test
    fun `static web content dir not found`() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        webStatic: bogus
                """.trimIndent())
            }
        }
        val e = assertFailsWith<UserMistake> { ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml"))) }
        assertThat(e.message!!).contains("bogus")
    }
}
