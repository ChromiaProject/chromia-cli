package com.chromia.cli.command

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEmpty
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.testData
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.rell.api.base.RellCliBasicException
import net.postchain.rell.api.base.RellCliException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertFailsWith
import com.github.ajalt.clikt.core.CliktCommand
import io.mockk.*
import org.junit.jupiter.api.fail
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.absolutePathString

internal class BuildCommandTest {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

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
            BuildCommand().context {
                terminal = testTerminal
            }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml")))
        }
        assertThat(logger.stderr()).all {
            contains("Should be: 11")
            contains("Was: ${TestRepositoryCloner.bar.model.rid}")
        }
    }

    @Test
    fun `skip-lib-check should bypass tampered library verification`() {
        testData(dir.toPath()) {
            config {
                addLib("bar", TestRepositoryCloner.bar.model.copy(rid = "11".hexStringToWrappedByteArray()))
            }
        }

        TestRepositoryCloner().clone("http://bar.com", dir.resolve("src/lib/bar").toPath(), "")
        // Should NOT throw ValidationException when --skip-lib-check is used
        BuildCommand().context {
            terminal = testTerminal
        }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml"), "--skip-lib-check"))
    }

    @Test
    fun `skip-lib-check should bypass missing library verification`() {
        testData(dir.toPath()) {
            config {
                addLib("missing", RellLibraryModel("http://missing.com", path = "lib", rid = "11".hexStringToWrappedByteArray()))
            }
        }
        // Should NOT throw ValidationException about missing library when --skip-lib-check is used
        BuildCommand().context {
            terminal = testTerminal
        }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml"), "--skip-lib-check"))
    }

    @Test
    fun standardGtxModules() {
        command.parse()
        val outputFile = File(dir, "build/hello.xml")
        assertTrue(outputFile.exists())
        val outputGtv = GtvMLParser.parseGtvML(outputFile.readText())
        assertThat(outputGtv["gtx"]?.get("modules")?.asArray()?.map { it.asString() }!!).containsExactlyInAnyOrder(
                "net.postchain.rell.module.RellPostchainModuleFactory",
                "net.postchain.gtx.StandardOpsGTXModule"
        )
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

    private val rellSourceWithInvalidXmlChar = "\nmodule;\nquery signMessage() = \"\u0019Ethereum Signed Message:\\n\";"

    @Test
    fun invalidXmlChars() {
        File(dir, "src/main.rell").writeText(rellSourceWithInvalidXmlChar)

        assertFailsWith<RellCliException> {
            BuildCommand().context { terminal = testTerminal }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml")))
        }
        assertThat(logger.stderr()).contains("Syntax error")

        assertFalse(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun binaryGtvFormatRejectsInvalidXmlChars() {
        File(dir, "src/main.rell").writeText(rellSourceWithInvalidXmlChar)

        assertFailsWith<RellCliException> {
            BuildCommand().context { terminal = testTerminal }.parse(listOf("--settings", dir.absolutePath.plus("/chromia.yml"), "--format=GTV"))
        }

        assertFalse(File(dir, "build/hello.gtv").exists())
    }

    @Test
    fun `should hide library warnings with hide-lib-warnings option`() {
        val projectResourceUrl = javaClass.classLoader.getResource("dapp_with_libWarnings")
                ?: fail { "dapp_with_libWarnings not found" }
        val projectPath = Paths.get(projectResourceUrl.toURI())
        val chromiaYmlPath = projectPath.resolve("chromia.yml").absolutePathString()

        val terminalRecorder = TerminalRecorder()
        val terminal = Terminal(terminalInterface = terminalRecorder)
        
        BuildCommand().context { this.terminal = terminal }
            .parse(listOf("--settings", chromiaYmlPath))
        val outputWithoutHiding = terminalRecorder.stderr()

        terminalRecorder.clearOutput()
        
        BuildCommand().context { this.terminal = terminal }
            .parse(listOf("--settings", dir.resolve("chromia.yml").toString(), "--hide-lib-warnings"))
        val outputWithHiding = terminalRecorder.stderr()
        
        assertThat(outputWithoutHiding).contains("lib/testlib")
        assertThat(outputWithHiding).doesNotContain("lib/testlib")
    }

    @Test
    fun `test message routing in BuildCommandCliEnv`() {
        val mockCmd = mockk<CliktCommand>()
        every { mockCmd.echo(any(), any(), any()) } just Runs
        
        val env = BuildCliEnv(mockCmd, hideLibWarnings = false)
        
        val regularMsg = "This is a regular message"
        val userWarningMsg = "src/main.rell Warning: Missing semicolon"
        val libWarningMsg = "lib/external/helper.rell Warning: Deprecated function"
        val summaryMsg = "Errors: 3 Warnings: 7" 
        
        env.error(regularMsg)
        env.error(userWarningMsg)
        env.error(libWarningMsg)
        env.error(summaryMsg)
        
        verify(exactly = 1) { mockCmd.echo(regularMsg, any(), any()) }
        verify(exactly = 1) { mockCmd.echo(userWarningMsg, any(), any()) }
        verify(exactly = 1) { mockCmd.echo(libWarningMsg, any(), any()) }
        
        verify(exactly = 1) { 
            mockCmd.echo(
                withArg { summaryText: String ->
                    assertThat(summaryText).all {
                        contains("Errors: 3")
                        contains("User Warnings: 1")
                        contains("Lib Warnings: 6")
                    }
                },
                any(),
                any()
            ) 
        }
        
        assertEquals(1, env.userWarningCount)
    }
    
}
