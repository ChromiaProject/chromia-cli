package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQuery
import com.chromia.build.tools.testData
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.client.exception.ClientError
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvBigInteger
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvInteger
import net.postchain.gtv.GtvNull
import net.postchain.gtv.GtvString
import net.postchain.gtv.gtvml.GtvMLParser
import net.postchain.gtv.parse.GtvParser
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.math.BigInteger
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class QueryCommandTest : IntegrationTestSetup() {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File

    private val dummyApiUrl = "http://not_existing_host:7740"
    private val dummyBrid = "CF66169BF4D8D4F618D39A09F7C06B55EF5F4E1296BD934649295A69F7925D2C"
    private val chromiaConfigFile = ".chromia/config"

    private fun createTestNode(config: String) {
        val gtvConfig = GtvMLParser.parseGtvML(File(config).readText())
        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)
    }

    @BeforeEach
    fun setup() {
        testData(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @Test
    fun testMissingQueryName() {
        val res = QueryCommand().test(listOf())
        assertThat(res.stderr).contains("missing argument <queryname>")
    }

    @Test
    fun testIncorrectQueryOptionAndMissingArgument() {
        val res = QueryCommand().test(listOf("--bar"))
        assertThat(res.stderr).contains("no such option --bar")
        assertThat(res.stderr).contains("missing argument <queryname>")
    }

    @Test
    fun testCanNotFindSettings() {
        val res = QueryCommand().test(listOf("--settings=missingFile.yml", "hello_world"))
        assertThat(res.stderr).contains("invalid value for --settings: file \"missingFile.yml")
    }

    @Test
    fun testCanNotParseArgs() {
        val res = QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "hello", "arg1 -> 1"))
        assertThat(res.stderr).contains("invalid value for <args>: query must be done with named parameters in a dict")
    }

    @Test
    fun testMissingBrid() {
        val thrown = assertThrows<RuntimeException> {
            QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "--api-url", "http://localhost:7740", "hello"))
        }

        assertThat(thrown.message!!).contains("Could not auto-detect brid from")
    }

    @Test
    fun testLoadingFromChromiaConfigFile() {
        val configFile = File(testDir.toFile(), chromiaConfigFile).apply {
            parentFile.mkdirs()
            writeText("""
                brid=$dummyBrid
                api.url=$dummyApiUrl
            """.trimIndent())
        }
        val res = assertThrows<ClientError> {
            QueryCommand().test(listOf("--settings", settingsFile.absolutePath, "--config", configFile.absolutePath, "api_version"))
        }
        assertThat(res.message!!).contains(dummyApiUrl)
    }

    @Test
    fun testCommandLineArgOverridesChromiaConfigFile() {
        val configFile = File(testDir.toFile(), chromiaConfigFile).apply {
            parentFile.mkdirs()
            writeText("""
                brid=$dummyBrid
                api.url=$dummyApiUrl
            """.trimIndent())
        }
        val overrideApiUrl = "http://not_existing_host_from_command_line:7741"
        val res = assertThrows<ClientError> {
            QueryCommand().test(listOf("--api-url", overrideApiUrl, "--settings", settingsFile.absolutePath, "--config", configFile.absolutePath, "api_version"))
        }
        assertThat(res.message!!).contains(overrideApiUrl)
    }

    @Test
    fun arguments(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct my_struct { name; }
                query test_query(foo: integer, bar: text, baz: text, my_struct, n: integer?): integer {
                    require(foo == 17);
                    require(bar == "hello");
                    require(baz == "Hello, world=5");
                    require(my_struct.name == "what ever");
                    require(n == null);
                    return 4711;
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
                      blockstrategy:
                        name: net.postchain.devtools.OnDemandBlockBuildingStrategy

                database:
                  schema: txcommandtest0_0
            """.trimIndent())
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        createTestNode("${dir.absolutePathString()}/build/a.xml")
        val res = QueryCommand().test(listOf("test_query", "foo=17", "bar=hello", "baz=\"Hello, world=5\"", "my_struct=[\"name\":\"what ever\"]", "n=null"))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo("4711\n")
    }

    @Test
    fun underscoreArgumentIsNotParsedAsOption() {
        withModel(TestModel().withQuery("test_query", gtv(1))) {
            val res = QueryCommand().test(listOf("test_query", "--api-url", apiUrl, "_bar=hello"))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stderr).isEmpty()
            assertThat(res.stdout).contains("1\n")
        }
    }

    @Test
    fun doubleDashMakesForceArgumentParsing() {
        withModel(TestModel().withQuery("test_query", gtv(1))) {
            val res = QueryCommand().test(listOf("test_query", "--api-url", apiUrl, "--", "_bar=hello"))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stderr).isEmpty()
            assertThat(res.stdout).contains("1\n")
        }
    }

    private val queryResponse = gtv(mapOf("a" to gtv(listOf(gtv(1), gtv("foo bar"), GtvNull)), "b" to gtv(listOf(gtv("1234ABCD".hexStringToByteArray()), gtv(BigInteger("19223372036854775807"))))))

    @Test
    fun prettyPrint() {
        withModel(TestModel().withQuery("test_query", queryResponse)) {
            val res = QueryCommand().test(listOf("test_query", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            val output = res.stdout
            assertThat(output).isEqualTo("""[
            |  "a": [
            |    1,
            |    "foo bar",
            |    null
            |  ],
            |  "b": [
            |    x"1234ABCD",
            |    19223372036854775807L
            |  ]
            |]
            |""".trimMargin())
            assertThat(GtvParser.parse(output)).isEqualTo(queryResponse)
        }
    }

    @Test
    fun jsonOutput() {
        withModel(TestModel().withQuery("test_query", queryResponse)) {
            val res = QueryCommand().test(listOf("test_query", "-f", "json", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""{
            |  "a": [
            |    1,
            |    "foo bar",
            |    null
            |  ],
            |  "b": [
            |    "1234ABCD",
            |    19223372036854775807
            |  ]
            |}
            |""".trimMargin())
        }
    }

    @Test
    fun xmlOutput() {
        withModel(TestModel().withQuery("test_query", queryResponse)) {
            val res = QueryCommand().test(listOf("test_query", "-f", "xml", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
                |<dict>
                |    <entry key="a">
                |        <array>
                |            <int>1</int>
                |            <string>foo bar</string>
                |            <null xsi:nil="true" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"/>
                |        </array>
                |    </entry>
                |    <entry key="b">
                |        <array>
                |            <bytea>1234ABCD</bytea>
                |            <bigint>19223372036854775807</bigint>
                |        </array>
                |    </entry>
                |</dict>
                |""".trimMargin())
        }
    }

    @Test
    fun yamlOutput() {
        withModel(TestModel().withQuery("test_query", queryResponse)) {
            val res = QueryCommand().test(listOf("test_query", "-f", "yaml", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""---
            |a:
            |- 1
            |- foo bar
            |- null
            |b:
            |- x"1234ABCD"
            |- 19223372036854775807L
            |
            """.trimMargin())
        }
    }

    @Test
    fun rawOutputNull() {
        withModel(TestModel().withQuery("test_query", GtvNull)) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |null
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputByteArray() {
        withModel(TestModel().withQuery("test_query", GtvByteArray("1234ABCD".hexStringToByteArray()))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |0x1234abcd
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputString() {
        withModel(TestModel().withQuery("test_query", GtvString("""Stockholm Göteborg 😀"""))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |Stockholm Göteborg 😀
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputInteger() {
        withModel(TestModel().withQuery("test_query", GtvInteger(17))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |17
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputBigInteger() {
        withModel(TestModel().withQuery("test_query", GtvBigInteger(BigInteger("19223372036854775807")))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |19223372036854775807
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputArrayOfInteger() {
        withModel(TestModel().withQuery("test_query", gtv(listOf(gtv(1), gtv(2), gtv(3))))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |1
            |2
            |3
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputArrayOfString() {
        withModel(TestModel().withQuery("test_query", gtv(listOf(gtv("Stockholm"), gtv("Göteborg"), gtv("""😀"""))))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |Stockholm
            |Göteborg
            |😀
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputDictOfInteger() {
        withModel(TestModel().withQuery("test_query", gtv(mapOf("a" to gtv(1), "b" to gtv(2), "c" to gtv(3))))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |a=1
            |b=2
            |c=3
            |""".trimMargin())
        }
    }

    @Test
    fun rawOutputDictOfString() {
        withModel(TestModel().withQuery("test_query", gtv(mapOf("a" to gtv("Stockholm"), "b" to gtv("Göteborg"), "ö" to gtv("""😀"""))))) {
            val res = QueryCommand().test(listOf("test_query", "--output-format", "raw", "--api-url", apiUrl))
            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).isEqualTo("""
            |a=Stockholm
            |b=Göteborg
            |ö=😀
            |""".trimMargin())
        }
    }

    @Test
    fun testQueryWithBlockchainAndNetworkWithoutBrid() {
        val chainBrid = BlockchainRid.buildRepeat(8)
        with(File(testDir.toFile(), "chromia.yml")) {
            writeText("""
                blockchains:
                  my_library:
                    module: library
                deployments:
                  testnet: 
                    url: $apiUrl
                    chains:
                      my_library: x"$chainBrid"
            """.trimIndent())
        }

        withModel(TestQueryModel(chainBrid)) {
            val res = QueryCommand().test(listOf(
                    "--settings", settingsFile.absolutePath,
                    "--blockchain", "my_library",
                    "--network", "testnet",
                    "test_query"
            ))

            assertThat(res.statusCode).isEqualTo(0)
            assertThat(res.stdout).contains("SUCCESS")
            assertThat(res.stderr).isEmpty()
        }
    }
}


internal class TestQueryModel(val model: Model = TestModel()) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(apiUrl)))
            "test_query" -> gtv("SUCCESS")
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }
}
