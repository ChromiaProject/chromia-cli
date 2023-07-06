package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.Settings
import com.chromia.cli.util.TestConsole
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.core.context
import java.io.File
import java.nio.file.Path
import net.postchain.rell.api.base.RellCliBasicException
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


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
                    container: mycontainer
            """.trimIndent())
        }
        val secret = File(dir.toFile(), ".secret")
        with(secret) {
            writeText("""
privkey = BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114
pubkey = 03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05
            """.trimIndent())
        }
        val testConsole = TestConsole()
        val settings = Settings(settingsFile, parseModel(settingsFile))
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body(settings.compile.rellVersion))
        val throwable = assertThrows<RellCliBasicException> {
            DeployCreateCommand({ httpHandler }, mock()).context { console = testConsole }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "wrongConfig", "--network", "test", "--secret", secret.absolutePath))
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
        whenever(httpHandler.invoke(any())).thenReturn(Response(Status.OK, "").body("0.11.0"))
        val throwable = assertThrows<RellDeployVersionException> {
            DeployCreateCommand({ httpHandler }, mock()).context { console = testConsole }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "main", "--network", "test", "--secret", secretFile.absolutePath))
        }
        assertThat(throwable.message!!).contains("The local compile version 0.12.0 is not supported on the target network. Maximum version allowed is 0.11.0.\n" +
                "The deployment is aborted.")
    }
}
