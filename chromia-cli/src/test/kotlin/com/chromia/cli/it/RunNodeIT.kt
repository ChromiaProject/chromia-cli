package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.io.File
import java.nio.file.Path
import java.time.Duration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RunNodeIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition("Blockchain has been started")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start { process ->

                    TestProcess.Builder("query", "hello").startCondition("Hi!").start()
                    TestProcess.Builder("tx", "call_op", "1").startCondition("was posted WAITING: OK").start()

                    TestProcess.Builder("query", "new_query").startCondition("Unknown query: new_query").start()
                    with(File(dir.toFile(), "src/main.rell")) {
                        writeText("""
                            module;
                            query hello() = "Hi!";
                            query new_query() = 1;
                        """.trimIndent())
                    }
                    TestProcess.Builder("node", "update", "-n", "5")
                            .verbose()
                            .startCondition("Configuration added at height")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .start()
                    process.waitUntil("Blockchain has been started", Duration.ofSeconds(30))

                    TestProcess.Builder("query", "new_query").startCondition("1").start()

                    // Fail to update using configuration that already exists
                    testData(dir)
                    TestProcess.Builder("node", "update")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .startCondition("Blockchain configuration already exists in database, cannot update")
                            .start()
                }
    }
}
