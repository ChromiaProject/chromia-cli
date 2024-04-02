package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.devtools.IntegrationTestSetup
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
    fun arguments(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct my_struct { name; }
                operation test_op(s1: text, s2: text, my_struct, n: integer?) {
                    require(s1 == "foobar");
                    require(s2 == "Hello, world!");
                    require(my_struct.name == "foo bar");
                    require(n == null);
                }
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

                database:
                  schema: txcommandtest0_0
            """.trimIndent())
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        createTestNode("${dir.absolutePathString()}/build/a.xml")
        TxCommand().context { terminal = testTerminal }.parse(listOf("--await",
                "test_op", "foobar", "\"Hello, world!\"", "[\"foo bar\"]", "null",
                "-brid", "B59D412FC6D3C78F0941F117B8495FB78AD1D2C1711FD1DA5612E1A5BF5BFCB4"))

        assertThat(logger.output()).contains("CONFIRMED")
    }
}