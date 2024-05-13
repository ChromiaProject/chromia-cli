package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withQuery
import com.chromia.build.tools.testData
import com.github.ajalt.clikt.testing.test
import net.postchain.client.exception.ClientError
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
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
    fun underscoreArgumentIsParsedAsOption() {
        withModel(TestModel().withQuery("test_query", gtv(1))) {
            val res = QueryCommand().test(listOf("test_query", "--api-url", apiUrl, "_bar=hello"))
            assertThat(res.stderr).contains("Error: no such option _bar")
        }
    }

    @Test
    fun doubleDashMakesForceArgumentParsing() {
        withModel(TestModel().withQuery("test_query", gtv(1))) {
            val res = QueryCommand().test(listOf("test_query", "--api-url", apiUrl, "--", "_bar=hello"))
            assertThat(res.stderr).isEmpty()
        }
    }
}