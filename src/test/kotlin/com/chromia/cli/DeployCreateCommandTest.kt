package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.Settings
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
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

    @Test
    fun cannotDeployFaultyConfig(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct module_args { name; } 
            """.trimIndent())
        }
        val settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  wrongConfig: 
                    module: main
                    moduleArgs:
                      main:
                        name: { nameIsInterprededAsDict }
                deployments:
                  test:
                    url: "localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
            """.trimIndent())
        }
        with(File(dir.toFile(), ".secret")) {
            writeText("""
                pubkey = 12312312414124124124121
                privkey = 000000000000000000000000000000000000001
            """.trimIndent())
        }
        val testConsole = TestConsole()
        val settings = Settings(settingsFile, parseModel(settingsFile))
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.ACCEPTED, "").body(settings.compile.rellVersion))
        val throwable = assertThrows<CliktError> {
            DeployCreateCommand({ httpHandler }, mock()).context { console = testConsole }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "wrongConfig", "--network", "test"))
        }
        assertThat(throwable.message!!).contains("Bad module_args for module 'main': Decoding type 'text': expected STRING, actual DICT")
    }

    @Test
    fun cannotDeployNotMatchingRellVersion(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
            """.trimIndent())
        }
        val settingsFile = File(dir.toFile(), "config.yml").apply {
            writeText("""
                blockchains:
                  main:
                    module: main
                deployments:
                  test:
                    url: "http://foo.com"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000001"
                    container: foo
                compile:
                    rellVersion: 0.12.0
            """.trimIndent())
        }
        val secretFile = File(dir.toFile(), ".secret").apply {
            writeText("""
                pubkey = 12
                privkey = 00
            """.trimIndent())
        }
        val testConsole = TestConsole()
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.ACCEPTED, "").body("0.11.0"))
        val throwable = assertThrows<RellDeployVersionException> {
            DeployCreateCommand({ httpHandler }, mock()).context { console = testConsole }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "main", "--network", "test", "--secret", secretFile.absolutePath))
        }
        assertThat(throwable.message!!).contains("The local compile version 0.12.0 is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                "The deployment is aborted.")
    }
}
