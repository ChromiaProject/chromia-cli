package com.chromia.build.tools.model

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.model.parseModel
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvBigInteger
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvNull
import net.postchain.gtv.GtvString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigInteger
import java.nio.file.Path

internal class ChromiaModelTest {
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
