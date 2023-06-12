package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
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

class TxCommandTest: IntegrationTestSetup() {

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
        with(File(dir.toFile(), "config.yml")) {
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
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/config.yml"))
        createTestNode("${dir.absolutePathString()}/build/a.xml")
        nodes.forEach { it.buildBlocksUpTo(0, 0) }
        val testConsole = TestConsole()
        TxCommand().context { console = testConsole }.parse(listOf("pass_struct", "[\"foo\"]"
        , "-brid", "07B6213B67C03F107C6EE59B8015143DD68C7D87C707DEA971EA7F16A15EE053"))

        testConsole.assertContains("WAITING")
    }
}