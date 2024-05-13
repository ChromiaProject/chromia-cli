package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.build.tools.testData
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
                    val brid = "3A37F5FC551CEB067DCA05AAC78A1F7CFF36C7BE9EBB50246EBCF4B664311F21"
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid).startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", brid, "-l").startCondition("main").start()

                }
    }
}
