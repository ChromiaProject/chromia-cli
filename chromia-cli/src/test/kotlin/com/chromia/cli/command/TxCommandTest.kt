package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isZero
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQuery
import com.github.ajalt.clikt.testing.test
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
import java.time.Instant
import kotlin.io.path.absolutePathString


class TxCommandTest : IntegrationTestSetup() {
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
            operation test_text(s1: text, s2: text) {
                require(s1 == "foobar", "foobar expected");
                require(s2 == "foo bar");
            }
            operation test_numeric(s1: integer, s2: big_integer, s3: decimal) {
                require(s1 == 1);
                require(s2 == 2);
                require(s3 == 1.2);
            }
            operation test_nullable(s1: integer?) {
                require(s1 == null);
            }
            operation test_collection(s1: list<text>, s2: set<text>, s3: map<text, integer>) {
                require(s1 == ["foo", "bar"]);
                require(s3["foo"] == 1);
            }
            operation test_struct(s1: my_struct) {
                require(s1.name == "foo bar");
            }       
            operation test_byte_array(s1: byte_array) {
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
        var res = TxCommand().test(listOf("--await",
                "test_text", "foobar", "\"foo bar\"", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "test_numeric", "1", "2L", "\"1.2\"", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "test_nullable", "null", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "test_collection", "[\"foo\", \"bar\"]", "[\"foo\", \"bar\"]", "[\"foo\": 1]", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "test_struct", "[\"foo bar\"]", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "test_byte_array", "x\"0373599a61cc6b3bc02a78c34313e1737ae9cfd56b9bb24360b437d469efdf3b15\"", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")

        res = TxCommand().test(listOf("--no-await",
                "bogus_op", "-brid", "$brid"))
        assertThat(res.stdout).contains("Transaction was rejected immediately: Unknown operation: bogus_op")
        res = TxCommand().test(listOf("--await",
                "bogus_op", "-brid", "$brid"))
        assertThat(res.stdout).contains("Transaction was rejected immediately: Unknown operation: bogus_op")
        res = TxCommand().test(listOf("--await",
                "test_text", "bogus", "\"foo bar\"", "-brid", "$brid"))
        assertThat(res.stdout).contains("Transaction was rejected after polling: [main:test_text(main.rell:4)] Operation 'main:test_text' failed: foobar expected")
    }

    @Test
    fun wrongFormatOfBridForIccfSourceThrowsError() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--iccf-source", "00"))
            assertThat(res.stderr).contains("invalid value for --iccf-source: Wrong size of Blockchain RID, was 1 should be 32 (64 characters)")
        }
    }

    @Test
    fun awaitTxByDefault() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun noAwaitTxGivesCorrectStatusCode() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--no-await"))
            assertThat(res.stdout).contains("was posted but is still pending")
        }
    }

    @Test
    fun underscoreArgumentIsParsed() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--await", "--api-url", apiUrl, "_foobar"))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun underscoreOperationIsParsed() {
        withModel(TestModel().withQuery("_test_op", gtv(1))) {
            val res = TxCommand().test(listOf("_test_op", "--await", "--api-url", apiUrl, "foobar"))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun timebAt() {
        val timeb = Instant.now().plusSeconds(60).toEpochMilli()
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--timeb-at", timeb.toString()))
            assertThat(res.statusCode).isZero()
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun timebAfter() {
        withModel(TestModel().withQuery("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--timeb-after", "60"))
            assertThat(res.statusCode).isZero()
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }
}
