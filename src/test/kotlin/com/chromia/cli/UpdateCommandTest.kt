package com.chromia.cli

import assertk.assert
import assertk.assertions.isNotNull
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadConnection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.buildBlocksUpTo
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.assertThrows

internal class UpdateCommandTest : IntegrationTestSetup() {


    private fun createTestNode(config: String) {
        val gtvConfig = GtvMLParser.parseGtvML(File(config).readText())
        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        createNodesFromSystemSetup(setup, true)
    }

    @Test
    fun `Start and update a dapp`(@TempDir dir: Path) {
        // TODO: Add signer and schema dynamically
        with(File(dir.toFile(), "config.yml")) {
            writeText("""
                blockchains:
                  dummy:
                    module: main
                    config:
                      signers:
                        - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
                      blockstrategy:
                        name: net.postchain.devtools.OnDemandBlockBuildingStrategy
                    
                database:
                  schema: updatecommandtest0_0
            """.trimIndent())
        }
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                object my_name {
                  mutable name= "World";
                 }

                operation set_name(name) {
                  my_name.name = name;
                }

                query hello() = "Hello %s!".format(my_name.name);
            """.trimIndent())
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/config.yml"))
        createTestNode("${dir.absolutePathString()}/build/dummy.xml")
        nodes.forEach { it.buildBlocksUpTo(0, 0) }
        val testConsole = TestConsole()
        UpdateCommand().context { console = testConsole }.parse(listOf("-s", "${dir.absolutePathString()}/config.yml"))
        testConsole.assertContains("Configuration added at height 2\n")
        nodes.forEach {
            withReadConnection(it.postchainContext.storage, 0) { ctx ->
                assert(BlockchainApi.getConfiguration(ctx, 2)).isNotNull()
            }
        }
        assertThrows<CliktError> { UpdateCommand().parse(listOf("-s", "${dir.absolutePathString()}/config.yml", "-n", "1")) }
    }
}
