package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.it.TestDataCreator
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.testing.test
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
import java.io.File
import java.nio.file.Path


class DeployCreateCommandTest {
    private val httpHandler = mock<HttpHandler>()
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var secret: File

    @BeforeEach
    fun setup(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        testDir = dir
        settingsFile = testDir.resolve("config.yml").toFile()
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
            DeployCreateCommand({ httpHandler }, mock()).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun cannotDeployNotMatchingRellVersion() {
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.11.0"))
        val throwable = assertThrows<RellDeployVersionException> {
            DeployCreateCommand({ httpHandler }, mock()).parse(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("The local compile version 0.12.0 is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                "The deployment is aborted.")
    }

    @Test
    fun parseResumeAttributeChainMissing(@TempDir dir: Path) {
        val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "missing chain", "--network", "test"))
        assertThat(res.stderr).contains("Error: invalid value for --blockchain: Specified blockchain(s) [missing chain] does not exist")
    }

    @Test
    fun parseResumeAttributeNetworkMissing(@TempDir dir: Path) {
        val res = DeployCreateCommand().test(listOf("-s", settingsFile.absolutePath, "--secret", secret.absolutePath, "--blockchain", "hello", "--network", "missing Network"))
        assertThat(res.stderr).contains("Error: invalid value for --network: Specified target [missing Network] does not exist")
    }
}
