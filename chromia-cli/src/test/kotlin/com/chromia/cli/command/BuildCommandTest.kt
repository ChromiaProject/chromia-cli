package com.chromia.cli.command

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import java.io.File
import kotlin.test.assertFailsWith
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.api.base.RellCliBasicException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

internal class BuildCommandTest {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)

    @RegisterExtension
    val command = CommandExtension(BuildCommand().context { terminal = testTerminal })
    val dir get() = command.dir

    @Test
    fun testCanNotFindSettings() {
        val res = BuildCommand().test(listOf())
        assertThat(res.output).contains("Project settings file not found")
    }

    @Test
    fun testMultipleRun() {
        command.parse()
        command.parse()
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun testSingleRun() {
        command.parse()

        assertEquals(File(dir, "build").list()?.size ?: 0, 1)
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun failsValidation() {
        testData(dir.toPath()) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        moduleArgs:
                          main:
                            my_args: "wrong_type"
            """.trimIndent())
            }
            content("""
            module;
            struct module_args { my_args: integer; }
            """.trimIndent())
        }
        val e = assertFailsWith<RellCliBasicException> { command.parse() }
        assertThat(e.message!!).contains("Bad module_args for module 'main': Decoding type 'integer': expected INTEGER, actual STRING")
    }

    @Test
    fun simpleLibraryExistTest() {
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/lib/bar").toPath(), "")
        command.parse()
        assertThat(dir.resolve("src/lib/bar").listFiles()).isNotEmpty()
    }

    @Test
    fun libraryMissingTest() {
        testData(dir.toPath()) {
            config {
                addLib("missing", RellLibraryModel("http://missing.com", path = "lib", rid = "11".hexStringToWrappedByteArray()))
            }
        }
        val e = assertFailsWith<ValidationException> { command.parse() }
        assertThat(e.message!!).contains("Library missing is not installed, install before building")
    }

    @Test
    fun libraryTamperedTest() {
        testData(dir.toPath()) {
            config {
                addLib("bar", TestRepositoryCloner.bar.model.copy(rid = "11".hexStringToWrappedByteArray()))
            }
        }

        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/lib/bar").toPath(), "")
        assertFailsWith<ValidationException> {
            BuildCommand().context { terminal = testTerminal }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml")))
        }
        assertThat(logger.stderr()).all {
            contains("Should be: 11")
            contains("Was: ${TestRepositoryCloner.bar.model.rid}")
        }
    }

    @Test
    fun whiteListedGtxModules() {
        File(dir, "chromia.yml").writeText("""
            blockchains:
              hello:
                module: main
                config:
                  gtx:
                    modules:
                      - "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule"
                      - "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule"
                      - "net.postchain.d1.icmf.IcmfSenderGTXModule"
                      - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                      - "net.postchain.d1.iccf.IccfGTXModule"
        """.trimIndent())
        File(dir, "src/main.rell").writeText("""
            module;
        """.trimIndent())
        command.parse()
        val outputFile = File(dir, "build/hello.xml")
        assertTrue(outputFile.exists())
        val outputGtv = GtvMLParser.parseGtvML(outputFile.readText())
        assertThat(outputGtv["gtx"]?.get("modules")?.asArray()?.map { it.asString() }!!).containsAll(
                "net.postchain.d1.anchoring.system.SystemAnchoringGTXModule",
                "net.postchain.d1.anchoring.cluster.ClusterAnchoringGTXModule",
                "net.postchain.d1.icmf.IcmfSenderGTXModule",
                "net.postchain.d1.icmf.IcmfReceiverGTXModule",
                "net.postchain.d1.iccf.IccfGTXModule"
        )
    }

    @Test
    fun convertBridFieldForIcmf() {
        File(dir, "chromia.yml").writeText("""
            blockchains:
              hello:
                module: main
                config:
                  shouldStillExist: true
                  icmf:
                    receiver:
                      local:
                        - topic: "L_delivery"
                          brid: null
                        - topic: "L_shipment_ready"
                          brid: null
                  gtx:
                    modules:
                      - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                  sync_ext:
                    - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"
                   
        """.trimIndent())
        File(dir, "src/main.rell").writeText("""
            module;
        """.trimIndent())

        command.parse()
        val outputFile = File(dir, "build/hello.xml")

        val outputGtv = GtvMLParser.parseGtvML(outputFile.readText())
        val localAttribute = outputGtv["icmf"]?.get("receiver")?.get("local")?.asArray()
        assertThat(localAttribute!!.size).isEqualTo(2)
        assertThat(localAttribute[0].asDict().containsKey("bc-rid")).isTrue()
        assertThat(localAttribute[0].asDict().containsKey("brid")).isFalse()
        assertThat(localAttribute[1].asDict().containsKey("bc-rid")).isTrue()
        assertThat(localAttribute[1].asDict().containsKey("brid")).isFalse()
        assertThat(outputGtv["shouldStillExist"]).isNotNull()
    }
}
