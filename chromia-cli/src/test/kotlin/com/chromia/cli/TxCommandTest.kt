package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.buildBlocksUpTo
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class TxCommandTest : IntegrationTestSetup() {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)

    private fun createTestNode(config: String) {
        val gtvConfig = GtvMLParser.parseGtvML(File(config).readText())
        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)
    }

    @Test
    fun sendStructAsArgument(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct my_arg { name; }
                operation pass_struct(my_arg) {}
            """.trimIndent())
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText("""
                blockchains:
                  a:
                    module: main
                    config:
                      signers:
                        - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
                      blockstrategy:
                        name: net.postchain.devtools.OnDemandBlockBuildingStrategy

                database:
                  schema: txcommandtest0_0
            """.trimIndent())
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        createTestNode("${dir.absolutePathString()}/build/a.xml")
        nodes.forEach { it.buildBlocksUpTo(0, 0) }
        TxCommand().context { terminal = testTerminal }.parse(listOf("pass_struct", "[\"foo\"]", "-brid", "94218C10C0F4D1216D4C5379C77C0E9AADCA61949F7AFB053A83E7B5A2F483B8"))

        assertThat(logger.output()).contains("WAITING")
    }
}