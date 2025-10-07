package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isZero
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQueryWithHeight
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.absolutePathString


class TxCommandTest : IntegrationTestSetup() {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    private fun launchBlockchainsInTestNode(vararg configs: String): List<BlockchainRid> {
        val gtvConfigs = configs.map { GtvMLParser.parseGtvML(File(it).readText()) }
        val setup = SystemSetupFactory.buildSystemSetup(gtvConfigs.mapIndexed { i, it -> BlockchainSetup.buildFromGtv(i, it) })
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)
        return setup.blockchainMap.toList().sortedBy { it.first }.map { it.second.rid }
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
            operation test_iccf_first(tx_to_prove: gtx_transaction, op_index: integer, other_arg: text) {
            }
            operation test_iccf_second(other_arg: text, tx_to_prove: gtx_transaction, op_index: integer) {
            }
            
            // Mock ICCF
            operation iccf_proof(blockchain_rid: byte_array, tx_hash: byte_array, tx_proof: byte_array) {}
            query cm_get_blockchain_api_urls(blockchain_rid: byte_array) = [ "http://localhost:7740" ];
            query cm_get_blockchain_cluster(brid: byte_array) = "my_cluster";            
            """.trimIndent())
        }

        with(File(dir.toFile(), "src/other.rell")) {
            parentFile.mkdirs()
            writeText("""
            module;
            operation op_to_prove(arg: integer) {
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

                  b:
                    module: other
                    config:
                      signers:
                        - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"

                database:
                  schema: txcommandtest0_0
            """.trimIndent())
        }

        BuildCommand().test(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val blockchainRids = launchBlockchainsInTestNode("${dir.absolutePathString()}/build/a.xml", "${dir.absolutePathString()}/build/b.xml")
        val brid = blockchainRids[0]
        val otherBrid = blockchainRids[1]
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

        res = TxCommand().test(listOf("--await", "op_to_prove", "1", "-brid", "$otherBrid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        val txToProve = res.stdout.substring("transaction with rid ".length, "transaction with rid ".length + 64)
        res = TxCommand().test(listOf("--await",
                "--iccf-tx", txToProve,
                "--iccf-source", otherBrid.toHex(),
                "--source-api-url", "http://localhost:7740",
                "test_iccf_first", "0", "\"foo bar\"", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
        res = TxCommand().test(listOf("--await",
                "--iccf-tx", txToProve,
                "--iccf-source", otherBrid.toHex(),
                "--source-api-url", "http://localhost:7740",
                "--iccf-arg-pos", "1",
                "test_iccf_second", "\"foo bar\"", "0", "-brid", "$brid"))
        assertThat(res.stdout).contains("was posted and confirmed")
    }

    @Test
    fun wrongFormatOfBridForIccfSourceThrowsError() {
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--iccf-source", "00"))
            assertThat(res.stderr).contains("invalid value for --iccf-source: Wrong size of Blockchain RID, was 1 should be 32 (64 characters)")
        }
    }

    @Test
    fun awaitTxByDefault() {
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun noAwaitTxGivesCorrectStatusCode() {
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--no-await"))
            assertThat(res.stdout).contains("was posted but is still pending")
        }
    }

    @Test
    fun underscoreArgumentIsParsed() {
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--await", "--api-url", apiUrl, "_foobar"))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun underscoreOperationIsParsed() {
        withModel(TestModel().withQueryWithHeight("_test_op", gtv(1))) {
            val res = TxCommand().test(listOf("_test_op", "--await", "--api-url", apiUrl, "foobar"))
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun timebAt() {
        val timeb = Instant.now().plusSeconds(60).toEpochMilli()
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--timeb-at", timeb.toString()))
            assertThat(res.statusCode).isZero()
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun timebAfter() {
        withModel(TestModel().withQueryWithHeight("test_op", gtv(1))) {
            val res = TxCommand().test(listOf("test_op", "--api-url", apiUrl, "--timeb-after", "60"))
            assertThat(res.statusCode).isZero()
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }

    @Test
    fun testNoBlockchainsConfiguredError() {
        settingsFile.writeText(
            """
            deployments:
              test-network:
                url:
                  - "http://localhost:7740"
            """.trimIndent()
        )

        val res = assertThrows<CliktError> {
            TxCommand().parse(
                    listOf(
                            "--settings", settingsFile.absolutePath,
                            "--network", "test-network",
                            "test_op"
                    )
            )
        }
        assertThat(res.message!!).isEqualTo("""
            No blockchains configured for deployment 'test-network'
            Resolution options:
            - Add blockchain configurations to your deployment
            - Choose a different deployment target using --network
         """.trimIndent()
        )
    }

    @Test
    fun testMultipleBlockchainsAvailableError() {
        settingsFile.writeText(
            """
           deployments:
              test_network:
                brid: x"0000000000000000000000000000000000000000000000000000000000000002"
                url:
                  - "http://localhost:7740"
                chains:
                  blockchain1: x"0000000000000000000000000000000000000000000000000000000000000001"
                  blockchain2: x"0000000000000000000000000000000000000000000000000000000000000003"
            """.trimIndent()
        )

        val res = assertThrows<CliktError> {
            TxCommand().parse(
                    listOf(
                            "--settings", settingsFile.absolutePath,
                            "--network", "test_network",
                            "test_op"
                    )
            )
        }
        assertThat(res.message!!).isEqualTo("""
                     Multiple blockchains available in deployment 'test_network'
                     Available chains: blockchain1, blockchain2
                     Resolution:
                     - Specify the target blockchain using: --blockchain <name>
                         - Example: --blockchain blockchain1
                     """.trimIndent()
        )
    }

    @Test
    fun testInferBlockchainFromDeploymentCorrectly() {
        val blockchainRid = BlockchainRid.buildRepeat(1)
        settingsFile.writeText(
                """
            deployments:
              test-network:
                url: $apiUrl
                chains:
                  blockchain1: x"${blockchainRid.toHex()}"
            """.trimIndent()
        )

        withModel(TestTxModel(blockchainRid)) {
            val res = TxCommand().test(
                listOf(
                    "--settings",
                    settingsFile.absolutePath,
                    "--network",
                    "test-network",
                    "test_op"
                )
            )
            assertThat(res.stdout).contains("was posted and confirmed")
        }
    }
}

internal class TestTxModel(val model: Model = TestModel()) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)

    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> = query(query) to 0

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(apiUrl)))
            "test_op" -> gtv("SUCCESS")
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }
}
