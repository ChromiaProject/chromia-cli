package com.chromia.cli.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolutePathString

class StartCommandIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        with(File(dir.toFile(), "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                query hello() = "Hi!";
            """.trimIndent())
        }
        with(File(dir.toFile(), "config.yml")) {
            writeText("""
                blockchains:
                  hello:
                    module: main
            """.trimIndent())
        }
        val process = ProcessBuilder("./chr", "node", "start", "-s", "${dir.absolutePathString()}/config.yml", "--wipe")
                .apply {
                    redirectErrorStream(true)
                }.start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))

        var blockchainHasStarted = false
        val startTime = System.currentTimeMillis()
        val timeout = TimeUnit.SECONDS.toMillis(10)
        while (!blockchainHasStarted && System.currentTimeMillis() - startTime < timeout) {
            reader.readLine()?.let {
                println(it)
                if (it.contains("Blockchain has been started")) {
                    blockchainHasStarted = true
                }
            }
        }
        process.destroy()
        assertThat(blockchainHasStarted).isTrue()
    }
}
