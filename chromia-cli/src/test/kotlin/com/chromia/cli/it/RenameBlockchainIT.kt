package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import net.postchain.api.rest.controller.BLOCKCHAIN_RID
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class RenameBlockchainIT {
    @Test
    fun proposeRenameBlockchain(@TempDir dir: Path) {
        val secretFile = File(dir.toFile(), ".secret")
        testData(dir) {
            config {
                addDeploymentsConfig()
            }
            secret {
                secretFile(dir)
            }
        }
        withModel(DirectoryChainModel(apiVersion = 65)) {
            TestProcess.Builder(
                    "deployment", "proposal", "rename",
                    "--network", "test",
                    "-n", "New Blockchain name",
                    "--description", "Proposed name",
                    "--secret", secretFile.absolutePath,
                    "-brid", "${BlockchainRid.ZERO_RID}"
            )
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .verbose()
                    .startCondition("Blockchain rename proposition was added successfully")
                    .start()
        }
    }
}