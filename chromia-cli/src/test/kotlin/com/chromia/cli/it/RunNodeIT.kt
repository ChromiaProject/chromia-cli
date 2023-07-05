package com.chromia.cli.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.TimeUnit

class RunNodeIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .setConfig(dir.resolve("config.yml").toFile())
                .start { process ->
                    process.waitUntil("Blockchain has been started", Duration.ofSeconds(30))

                    ChrProcess.Builder("query", "hello").start { assertThat(it.readLines()).containsExactly("\"Hi!\"") }
                    ChrProcess.Builder("tx", "call_op", "1").start { assertThat(it.readLines()).anyMatch { str -> str.contains("was posted WAITING: OK") } }

                    ChrProcess.Builder("query", "new_query").start {
                        assertThat(it.readLines()).containsExactly("query: 400 Bad Request  Unknown query: new_query from http://localhost:7740")
                    }
                    with(File(dir.toFile(), "src/main.rell")) {
                        writeText("""
                            module;
                            query hello() = "Hi!";
                            query new_query() = 1;
                        """.trimIndent())
                    }
                    ChrProcess.Builder("node", "update")
                            .setConfig(dir.resolve("config.yml").toFile())
                            .start {
                                it.waitUntil("onfiguration added at height", Duration.ofSeconds(10))
                            }
                    process.waitUntil("Blockchain has been started", Duration.ofSeconds(30))

                    ChrProcess.Builder("query", "new_query").start { assertThat(it.readLines()).containsExactly("1") }
                }
    }
}
