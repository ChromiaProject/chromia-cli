package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQuery
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.common.BlockchainRid
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString


class TxCommandTest : IntegrationTestSetup() {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

    private fun launchBlockchainInTestNode(config: String): BlockchainRid {
        val gtvConfig = GtvMLParser.parseGtvML(File(config).readText())
        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)
        return setup.blockchainMap[0]!!.rid
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

        BuildCommand().test(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val brid = launchBlockchainInTestNode("${dir.absolutePathString()}/build/a.xml")
        TxCommand().context { terminal = testTerminal }.parse(listOf("--await",
                "test_op", "foobar", "\"Hello, world!\"", "[\"foo bar\"]", "null",
                "-brid", "$brid"))

        assertThat(logger.output()).contains("CONFIRMED")
    }

    @Test
    fun awaitTxByDefault() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl))
            assertThat(res.stdout).contains("CONFIRMED")
        }
    }

    @Test
    fun noAwaitTxGivesCorrectStatusCode() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--no-await"))
            assertThat(res.stdout).contains("WAITING")
        }
    }


    @Test
    fun underscoreArgumentIsParsed() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--await", "--api-url", apiUrl, "_foobar"))
            assertThat(res.stdout).contains("CONFIRMED")
        }
    }

    @Test
    fun underscoreOperationIsParsed() {
        withModel(TestModel().withQuery("_test_op", gtv(1))) {
            val res = TxCommand().test(listOf("_test_op", "--await", "--api-url", apiUrl, "foobar"))
            assertThat(res.stdout).contains("CONFIRMED")
        }
    }
}