package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.TestClient
import com.chromia.cli.versionfinder.CanNotFindBlockchainException
import com.chromia.cli.versionfinder.NoNodeRunningContainerException
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.notExists
import kotlin.test.assertNotNull
import net.postchain.crypto.KeyPair
import net.postchain.rell.api.base.RellCliBasicException
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables


class DeployCreateCommandTest {
    private val httpHandler = mock<HttpHandler>()

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File
    private lateinit var config: File

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
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body(settings.compile.rellVersion))
        val throwable = assertThrows<RellCliBasicException> {
            DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun cannotDeployNotMatchingRellVersion() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.11.0"))
        val throwable = assertThrows<RellDeployVersionException> {
            DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("The local compile version 0.13.5 is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                "The deployment is aborted.")
    }

    @Test
    fun cannotDeployIfNodeIsUnresponsive() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.BAD_GATEWAY, "").body(""))
        val throwable = assertThrows<NoNodeRunningContainerException> {
            DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("No nodes found running the container \"foo\"")
    }

    @Test
    fun cannotDeployIfBridIsNotFound() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.NOT_FOUND, "").body(""))
        val throwable = assertThrows<CanNotFindBlockchainException> {
            DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Can not find blockchain with blockchainRID: 0000000000000000000000000000000000000000000000000000000000000001 on node http://node1_url")
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
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.13.5"))
        DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y"))

        val buildGtx = testDir.resolve("build").listDirectoryEntries().find { it.name.startsWith("test_my_rell_dapp") }?.toFile()?.readText()
        assertNotNull(buildGtx)
        val buildGtxCompressed = testDir.resolve("build/my_rell_dapp_compressed.xml").toFile().readText()
        assertThat(buildGtxCompressed).contains("<entry key=\"compressed_roots\">")
        assertThat(buildGtxCompressed).doesNotContain("<entry key=\"a_file.rell\">")
        assertThat(buildGtx).doesNotContain("<entry key=\"compressed_roots\">")
        assertThat(buildGtx).contains("<entry key=\"a_file.rell\">")
    }


    @Test
    fun noCompressionConfigGetsAddedToXmlConfig() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.13.5"))
        DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y", "--no-compression"))
        assertThat(testDir.resolve("build/my_rell_dapp_compressed.xml").notExists())
    }

    @Test
    fun deployDappUsingKeyId() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.13.5"))
        val keyPair = KeyPair.of("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765", "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        EnvironmentVariables("CHROMIA_HOME", testDir.absolutePathString()).execute {
            ChromiaKeyStore(DeploymentTestDataCreator.keyIdName).saveKeyPair(keyPair)
            val res = DeployCreateCommand({ httpHandler }, { TestClient(it, { 0 }) }).test(listOf("-s", settingsFile.absolutePath, "--blockchain", "my_rell_dapp", "--network", "test", "-y", "--config", config.absolutePath))
            assertThat(res.stdout).contains("Deployment of blockchain my_rell_dapp was successful")
        }
    }
}
