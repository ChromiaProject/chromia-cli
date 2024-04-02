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
                    val brid = "F0820C8D5757B179FC78E580F8A73A00DE19DFABDA491F92CA21F15B8DECB742"
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid).startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid, "-l").startCondition("main").start()

                }
    }
}
