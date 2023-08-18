package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.io.File
import java.nio.file.Path
import java.time.Duration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RunNodeIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .verbose()
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start { process ->
                    process.waitUntil("height: 0", Duration.ofSeconds(30))

                    TestProcess.Builder("query", "hello").start { assertThat(it.readLines()).containsExactly("\"Hi!\"") }
                    TestProcess.Builder("tx", "call_op", "1").start { assertThat(it.readLines()).anyMatch { str -> str.contains("was posted WAITING: OK") } }

                    TestProcess.Builder("query", "new_query").start {
                        assertThat(it.readLines()).containsExactly("query: 400 Bad Request  Unknown query: new_query from http://localhost:7740")
                    }
                    with(File(dir.toFile(), "src/main.rell")) {
                        writeText("""
                            module;
                            query hello() = "Hi!";
                            query new_query() = 1;
                        """.trimIndent())
                    }
                    TestProcess.Builder("node", "update")
                            .verbose()
                            .startCondition("Configuration added at height")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .start()
                    process.waitUntil("Blockchain has been started", Duration.ofSeconds(30))

                    TestProcess.Builder("query", "new_query").start { assertThat(it.readLines()).containsExactly("1") }

                    // Fail to update using configuration that already exists
                    testData(dir)
                    TestProcess.Builder("node", "update")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .start {
                                assertThat(it.readLines()).anyMatch { it.contains("Blockchain configuration already exists in database, cannot update") }
                            }
                }
    }
}
