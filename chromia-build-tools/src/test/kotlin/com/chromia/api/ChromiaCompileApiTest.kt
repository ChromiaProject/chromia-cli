package com.chromia.api

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.build.tools.lib.DirectoryHashCalculator.RidStrategy
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.model.parseModel
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
            addFile("lib/my/module.rell", """module;""")
            addFile("lib/my/test/module.rell", """@test module;""")
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
            addFile("lib/my/module.rell", """module; import lib.other;""")
            addFile("lib/other/module.rell", """module;""")
            addFile("lib/other/broken.rell", """module; import lib.nonexistent;""") // This file is not imported from lib.my
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val rid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src/lib/my"), RidStrategy.LIST)
        assertThat(result.first().config["rid"]?.asByteArray()?.wrap()).isEqualTo(rid)
    }

    @Test
    fun `Library with mismatching name throws`() {
        testData(dir) {
            config {
                blockchains("""
                blockchains:
                  wrong-name:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addFile("lib/my/module.rell", """module;""")
        }
        val err = assertThrows<IllegalArgumentException> {
            ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        }
        assertThat(err.message).isEqualTo("Library wrong-name not found. Please verify that the name of the library matches the folder name in lib folder.")
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
            addFile("lib/my/other.rell", """module; function f() =;""")
            addFile("lib/my/module.rell", """module;""") // Note that *other* is not imported
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
            addFile("lib/my/other.rell", """module; import lib.missing;""")
            addFile("lib/my/module.rell", """module;""") // Note that *other* is not imported
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
            addFile("lib/my/module.rell", """module;""")
            addFile("lib/my/inner/module.rell", """module;""")
            addFile("lib/my/test/utils.rell", """@test module;""")
            addFile("tests/lib_my_test.rell", """@test module; import lib.my.test.utils; function test() {}""")
        }
        val result = ChromiaCompileApi.build(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result.size).isEqualTo(1)
        val rid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src/lib/my"), RidStrategy.LIST)
        assertThat(result.first().config["rid"]?.asByteArray()?.wrap()).isEqualTo(rid)
        val allFilesRid = DirectoryHashCalculator(dir.resolve("src")).compute(dir.resolve("src"), RidStrategy.LIST)
        assertThat(allFilesRid).isNotEqualTo(rid)
    }

    @Test
    fun `verify library`(@TempDir dir: Path) {
        testData(dir)
        val result = ChromiaCompileApi.verify(cliEnv, parseModel(dir.resolve("chromia.yml")))
        assertThat(result).isTrue()
    }
}
