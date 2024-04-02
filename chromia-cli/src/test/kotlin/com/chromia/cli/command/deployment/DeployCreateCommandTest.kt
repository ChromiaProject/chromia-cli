package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.TestClient
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.testing.test
import java.io.File
import java.nio.file.Path
import kotlin.io.path.notExists
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


class DeployCreateCommandTest {
    private val httpHandler = mock<HttpHandler>()

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        secret = testDir.resolve(".secret").toFile()
        settings = parseModel(settingsFile)

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

        val buildGtx = testDir.resolve("build/my_rell_dapp.xml").toFile().readText()
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
}
