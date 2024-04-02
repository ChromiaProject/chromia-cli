package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.cli.util.testData
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DeployInspectIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start{
                    val brid = "4D6232FF8DDA05FFFA66FF58C5E0DC652D165D071D8241A2FF6F85B3199EE6BC"
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid).startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid, "-l").startCondition("main").start()

                }
    }
}
