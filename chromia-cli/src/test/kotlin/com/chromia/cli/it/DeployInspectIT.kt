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
                    val brid = "82DDB37978F1FDC594FEBE8FE828D1F88D867A533F01E17DACCC2E2D2E05981C"
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid).startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid, "-l").startCondition("main").start()

                }
    }
}
