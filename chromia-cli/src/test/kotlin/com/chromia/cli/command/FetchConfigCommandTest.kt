package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.testing.test
import net.postchain.client.exception.ClientError
import net.postchain.devtools.IntegrationTestSetup
import net.postchain.devtools.utils.configuration.BlockchainSetup
import net.postchain.devtools.utils.configuration.system.SystemSetupFactory
import net.postchain.gtv.gtvml.GtvMLParser
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.web3j.utils.Files
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class FetchConfigCommandTest : IntegrationTestSetup() {
    @TempDir
    private lateinit var testDir: Path

    private val dummyApiUrl = "http://not_existing_host:7740"
    private val dummyBrid = "CF66169BF4D8D4F618D39A09F7C06B55EF5F4E1296BD934649295A69F7925D2C"
    private val chromiaConfigFile = ".chromia/config"

    @Test
    fun testMissingOptions() {
        val res = FetchConfigCommand().test(listOf())
        assertThat(res.stderr).contains("Need to specify either --blockchain-config or --blockchain-rid")
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
            FetchConfigCommand().test(listOf("--config", configFile.absolutePath, "--blockchain-rid", dummyBrid))
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
            FetchConfigCommand().test(listOf("--api-url", overrideApiUrl, "--config", configFile.absolutePath, "--blockchain-rid", dummyBrid))
        }
        assertThat(res.message!!).contains(overrideApiUrl)
    }

    private val sourceRell = """
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
        """.trimIndent()

    private val sourceHtml = "<html></html>"

    private val sourceChromiaYml = """
            blockchains:
              a:
                module: main
                webStatic: web
                webCacheTtlSeconds: 17
                config:
                  signers:
                    - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
                  blockstrategy:
                    name: net.postchain.devtools.OnDemandBlockBuildingStrategy
                moduleArgs:
                  main:
                    foo: bar
            database:
              schema: txcommandtest0_0
        """.trimIndent()

    private val sourceChromiaYmlWithOnlyWeb = """
            blockchains:
              a:
                webStatic: web
                webCacheTtlSeconds: 17
                config:
                  signers:
                    - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
                  blockstrategy:
                    name: net.postchain.devtools.OnDemandBlockBuildingStrategy
            database:
              schema: txcommandtest0_0
        """.trimIndent()

    private val expectedConfigYml = """
    ---
    add_primary_key_to_header: 1
    blockstrategy:
      mininterblockinterval: 1000
      name: net.postchain.devtools.OnDemandBlockBuildingStrategy
    config_consensus_strategy: HEADER_HASH
    configurationfactory: net.postchain.gtx.GTXBlockchainConfigurationFactory
    gtx:
      modules:
      - net.postchain.rell.module.RellPostchainModuleFactory
      - net.postchain.web.WebStaticGTXModuleFactory
      - net.postchain.gtx.StandardOpsGTXModule
      rell:
        compilerVersion: 0.14.3
        moduleArgs:
          main:
            foo: bar
        modules:
        - main
        strictGtvConversion: 1
        version: 0.13.14
    revolt:
      fast_revolt_status_timeout: 2000
      revolt_when_should_build_block: 1
    signers:
    - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
    web_static:
      cache_ttl_seconds: 17

    """.trimIndent()

    private val expectedConfigYmlWithOnlyWeb = """
    ---
    add_primary_key_to_header: 1
    blockstrategy:
      mininterblockinterval: 1000
      name: net.postchain.devtools.OnDemandBlockBuildingStrategy
    config_consensus_strategy: HEADER_HASH
    configurationfactory: net.postchain.gtx.GTXBlockchainConfigurationFactory
    gtx:
      modules:
      - net.postchain.web.WebStaticGTXModuleFactory
      - net.postchain.gtx.StandardOpsGTXModule
    revolt:
      fast_revolt_status_timeout: 2000
      revolt_when_should_build_block: 1
    signers:
    - x"03A301697BDFCD704313BA48E51D567543F2A182031EFD6915DDC07BBCC4E16070"
    web_static:
      cache_ttl_seconds: 17

    """.trimIndent()

    private val expectedChromiaYml = """
    ---
    blockchains:
      main:
        module: main
        moduleArgs:
          main:
            foo: bar
        webCacheTtlSeconds: 17
        webStatic: web
    compile:
      rellVersion: 0.13.14

    """.trimIndent()

    private val expectedChromiaYmlWithOnlyWeb = """
    ---
    blockchains:
      bc:
        webCacheTtlSeconds: 17
        webStatic: web

    """.trimIndent()

    @Test
    fun fromNode(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText(sourceRell)
        }
        with(File(dir.toFile(), "web/index.html")) {
            parentFile.mkdirs()
            writeText(sourceHtml)
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText(sourceChromiaYml)
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val gtvConfig = GtvMLParser.parseGtvML(File("${dir.absolutePathString()}/build/a.xml").readText())
        val setup = SystemSetupFactory.buildSystemSetup(listOf(BlockchainSetup.buildFromGtv(0, gtvConfig)))
        setup.needRestApi = true
        createNodesFromSystemSetup(setup, true)
        val node = nodes.first()
        val res = FetchConfigCommand().test(listOf(
                "--blockchain-rid", node.getBlockchainRid(0)!!.toHex(),
                "--api-url", "http://localhost:${node.getRestApiHttpPort()}"
        ))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo(expectedConfigYml + "\n")
    }

    @Test
    fun fromFile(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText(sourceRell)
        }
        with(File(dir.toFile(), "web/index.html")) {
            parentFile.mkdirs()
            writeText(sourceHtml)
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText(sourceChromiaYml)
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val res = FetchConfigCommand().test(listOf(
                "--blockchain-config", "${dir.absolutePathString()}/build/a.xml"
        ))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo(expectedConfigYml + "\n")
    }

    @Test
    fun fromFileToDir(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText(sourceRell)
        }
        with(File(dir.toFile(), "web/index.html")) {
            parentFile.mkdirs()
            writeText(sourceHtml)
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText(sourceChromiaYml)
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val res = FetchConfigCommand().test(listOf(
                "--blockchain-config", "${dir.absolutePathString()}/build/a.xml",
                "--target", "${dir.absolutePathString()}/out"
        ))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo("""
            Saving blockchain config: blockchain-config.yml
            Creating chromia.yml
            Saving Rell source: src/main.rell
            Saving web resource: web/index.html

        """.trimIndent())
        assertThat(Files.readString(File(dir.toFile(), "out/blockchain-config.yml"))).isEqualTo(expectedConfigYml)
        assertThat(Files.readString(File(dir.toFile(), "out/chromia.yml"))).isEqualTo(expectedChromiaYml)
        assertThat(Files.readString(File(dir.toFile(), "out/src/main.rell"))).isEqualTo(sourceRell)
        assertThat(Files.readString(File(dir.toFile(), "out/web/index.html"))).isEqualTo(sourceHtml)
    }

    @Test
    fun fromFileToDirWithOnlyWeb(@TempDir dir: Path) {
        with(File(dir.toFile(), "web/index.html")) {
            parentFile.mkdirs()
            writeText(sourceHtml)
        }
        with(File(dir.toFile(), "chromia.yml")) {
            writeText(sourceChromiaYmlWithOnlyWeb)
        }
        BuildCommand().parse(listOf("-s", "${dir.absolutePathString()}/chromia.yml"))
        val res = FetchConfigCommand().test(listOf(
                "--blockchain-config", "${dir.absolutePathString()}/build/a.xml",
                "--target", "${dir.absolutePathString()}/out"
        ))
        assertThat(res.statusCode).isEqualTo(0)
        assertThat(res.stdout).isEqualTo("""
            Saving blockchain config: blockchain-config.yml
            Creating chromia.yml
            Saving web resource: web/index.html

        """.trimIndent())
        assertThat(Files.readString(File(dir.toFile(), "out/blockchain-config.yml"))).isEqualTo(expectedConfigYmlWithOnlyWeb)
        assertThat(Files.readString(File(dir.toFile(), "out/chromia.yml"))).isEqualTo(expectedChromiaYmlWithOnlyWeb)
        assertThat(Files.readString(File(dir.toFile(), "out/web/index.html"))).isEqualTo(sourceHtml)
    }
}
