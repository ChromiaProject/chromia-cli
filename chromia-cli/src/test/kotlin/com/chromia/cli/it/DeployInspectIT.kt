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
                    val brid = "F966BA68784376C2094C5464463712AF50DC3826B348CB5763A6D7EA11EE243C"
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid).startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid, "-l").startCondition("main").start()

                }
    }
}
