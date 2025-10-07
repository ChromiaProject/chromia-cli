package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.chromia.build.tools.restapi.*
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.ClusterManagementStub
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.versionfinder.NoNodeRunningContainerException
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.client.exception.ClientError
import net.postchain.common.BlockchainRid
import net.postchain.crypto.KeyPair
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.api.base.RellCliBasicException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.test.assertNotNull


class DeployCreateCommandTest {

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File
    private lateinit var config: File

    val model = DirectoryChainModel().withClusterManagement(ClusterManagementStub())

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        settings = parseModel(settingsFile)
        config = testDir.resolve("config").toFile()
    }

    @Test
    fun cannotDeployFaultyConfig() {
        with(File(testDir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct module_args { name; } 
            """.trimIndent())
        }
        withModel(model.withCompression().withRellVersion(settings.compile.langVersion)) {
            val throwable = assertThrows<RellCliBasicException> {
                DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
        }
    }

    @Test
    fun cannotDeployNotMatchingRellVersion() {
        withModel(model.withRellVersionWithHeight("0.11.0")) {
            val throwable = assertThrows<RellDeployVersionException> {
                DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("The local compile version $DefaultChromiaModelRellVersion is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                    "The deployment is aborted.")
        }
    }

    @Test
    fun cannotDeployIfNodeIsUnresponsive() {
        withModel(model.withQueryWithHeight("get_cluster_api_urls", gtv(gtv("http://not-responding")))) {
            val throwable = assertThrows<NoNodeRunningContainerException> {
                DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("No nodes found running the container \"foo\"")
        }
    }

    @Test
    fun cannotDeployIfBridIsNotFound() {
        withModel {
            val throwable = assertThrows<ClientError> {
                DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
            }
            assertThat(throwable.message!!).contains("Can't find blockchain with blockchainRID: 0000000000000000000000000000000000000000000000000000000000000000 from ${RestApiInstance.apiUrl}")
        }
    }

    @Test
    fun parseResumeAttributeChainMissing() {
        val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parseResumeAttributeNetworkMissing() {
        val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }

    @Test
    fun compressionConfigGetsAddedToXmlConfig() {
        withModel(
                model.withCompression()
                        .withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildRepeat(8)))
        ) {
            DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y"))
        }
        val buildGtx = testDir.resolve("build").listDirectoryEntries().find { it.name.startsWith("test_my_rell_dapp") }?.toFile()?.readText()
        assertNotNull(buildGtx)
        assertThat(buildGtx).contains("<entry key=\"compressed_roots\">")
    }


    @Test
    fun noCompressionConfigGetsAddedToXmlConfig() {
        withModel(
                model
                        .withCompression()
                        .withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildRepeat(8)))
        ) {
            DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y", "--no-compression"))
            assertThat(testDir.resolve("build/my_rell_dapp_compressed.xml").notExists())
            val buildGtx = testDir.resolve("build").listDirectoryEntries().find { it.name.startsWith("test_my_rell_dapp") }?.toFile()?.readText()
            assertNotNull(buildGtx)
            assertThat(buildGtx).doesNotContain("<entry key=\"compressed_roots\">")
            assertThat(buildGtx).contains("<entry key=\"a_file.rell\">")
        }
    }

    @Test
    fun deployDappUsingKeyId() {
        withModel(model
                .withCompression()
                .withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildRepeat(8)))
        ) {
            val keyPair = KeyPair.of("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765", "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
            EnvironmentVariables("CHROMIA_HOME", testDir.absolutePathString()).execute {
                File(testDir.absolutePathString(), "/config").also { it.parentFile.mkdirs() }.writeText("""
                    key.id = ${DeploymentTestDataCreator.keyIdName}
                """.trimIndent())
                ChromiaKeyStore(DeploymentTestDataCreator.keyIdName).saveKeyPair(keyPair)
                val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y", "--config", config.absolutePath))
                assertThat(res.stdout).contains("Deployment of blockchain my_rell_dapp was successful")
            }
        }
    }

    @Test
    fun deployAllDappsInConfig() {
        withModel(model.withCompression().withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildRepeat(8)))) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      foo:
                        module: main
                      bar:
                        module: main
                      fobar:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                        test:
                          url: "http://localhost:7745"
                          brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                          container: test_container
                """.trimIndent())
                }
            }
            val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--network", "test", "-y", "--config", config.absolutePath, "--secret", secret.absolutePath))
            assertThat(res.stdout).contains("Deployment of blockchain bar was successful")
            assertThat(res.stdout).contains("Deployment of blockchain foo was successful")
            assertThat(res.stdout).contains("Deployment of blockchain fobar was successful")
            assertThat(res.stdout).contains("""
                Add the following to your project settings file:
                deployments:
                  test:
                    chains:
                      foo: x"0808080808080808080808080808080808080808080808080808080808080808"
                      bar: x"0808080808080808080808080808080808080808080808080808080808080808"
                      fobar: x"0808080808080808080808080808080808080808080808080808080808080808"
            """.trimIndent())
        }
    }

    @Test
    fun `Can not create new deployment of chain defined under deployments`() {
        withModel(model.withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildRepeat(0)))) {
            val throwable = assertThrows<PrintMessage> {
                DeployCreateCommand().parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "deployed", "--network", "test"))
            }
            assertThat(throwable.message).isEqualTo("Blockchain 'deployed' is already defined in the configuration file: '${settingsFile.absoluteFile}' under the deployment: 'test'")
        }

    }

    @Test
    fun `should hide library warnings with hide-lib-warnings option`() {
        val projectResourceUrl = javaClass.classLoader.getResource("dapp_with_libWarnings")
                ?: fail { "dapp_with_libWarnings not found" }
        val projectPath = Paths.get(projectResourceUrl.toURI())
        val chromiaYmlPath = projectPath.resolve("chromia.yml").absolutePathString()

        val terminalRecorder = TerminalRecorder()
        val terminal = Terminal(terminalInterface = terminalRecorder, ansiLevel = AnsiLevel.NONE)

        withModel(model.withCompression()
                .withQueryWithHeight("find_blockchain_rid", gtv(BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002")))
        ) {
            val resultWithoutFlag = DeployCreateCommand().context { this.terminal = terminal }
                .test(listOf("-s", chromiaYmlPath, "--secret", secret.absolutePath, "--blockchain", "testlib", "--network", "test", "-y"))
            
            val outputWithoutHiding = resultWithoutFlag.stderr

            val resultWithFlag = DeployCreateCommand().context { this.terminal = terminal }
                .test(listOf("-s", projectPath.resolve("chromia.yml").absolutePathString(), "--secret", secret.absolutePath, "--blockchain", "testlib", "--network", "test", "-y", "--hide-lib-warnings"))

            val outputWithHiding = resultWithFlag.stderr


            assertThat(outputWithoutHiding).contains("lib/testlib")
            assertThat(outputWithHiding).doesNotContain("lib/testlib")

            assertThat(outputWithoutHiding).contains("Lib Warnings:")
            assertThat(outputWithHiding).contains("Lib Warnings:")
        }
    }
}
