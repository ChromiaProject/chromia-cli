package com.chromia.cli

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.isNotEmpty
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.lib.InstallDirTarget
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestRepositoryCloner
import com.chromia.cli.util.testData
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import java.io.File
import kotlin.test.assertFailsWith
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
        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/lib/bar"), "")
        command.parse()
        assertThat(dir.resolve("src/lib/bar").listFiles()).isNotEmpty()
    }

    @Test
    fun libraryMissingTest() {
        testData(dir.toPath()) {
            config {
                libs("""
                    libs:
                        missing:
                          registry: http://missing.com
                          path: lib
                          rid: x"11"
                """.trimIndent())
            }
        }
        val e = assertFailsWith<ValidationException> { command.parse() }
        assertThat(e.message!!).contains("Library missing is not installed, install before building")
    }

    @Test
    fun libraryTamperedTest() {
        testData(dir.toPath()) {
            config {
                libs("""
                    libs:
                        bar:
                          registry: http://bar.com
                          path: lib
                          rid: x"11"
                """.trimIndent())
            }
        }

        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/${InstallDirTarget.SOURCE.target}/bar"), "")
        assertFailsWith<ValidationException> {
            BuildCommand().context { terminal = testTerminal }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml")))
        }
        assertThat(logger.stderr()).all {
            contains("Should be: 11")
            contains("Was: 615175A2847D739C2CD0EC27339E8128549E513654069E2912A7E3C3E7032DB5")
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
}
