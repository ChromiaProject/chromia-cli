package com.chromia.cli.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit

class StartCommandIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .setConfig(dir.resolve("config.yml").toFile())
                .start { process ->
                    var blockchainHasStarted = false
                    val startTime = System.currentTimeMillis()
                    val timeout = TimeUnit.SECONDS.toMillis(10)
                    while (!blockchainHasStarted && System.currentTimeMillis() - startTime < timeout) {
                        process.readLine()?.let {
                            println(it)
                            if (it.contains("Blockchain has been started")) {
                                blockchainHasStarted = true
                            }
                        }
                    }
                    assertThat(blockchainHasStarted).isTrue()
                }
    }
}
