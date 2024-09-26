package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.createConfigurationFiles
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class RenameBlockchainIT {
    @Test
    fun proposeRenameBlockchain(@TempDir dir: Path) {
        testData(dir)
        val secretFile = File(dir.toFile(), ".secret")
        createConfigurationFiles(dir, secretFile)
        withModel(DirectoryChainModel(apiVersion = 65)) {
            TestProcess.Builder(
                    "deployment", "proposal", "rename",
                    "--network", "test",
                    "-n", "New Blockchain name",
                    "--description", "Proposed name",
                    "--secret", secretFile.absolutePath
            )
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .verbose()
                    .startCondition("Blockchain rename proposition was added successfully")
                    .start()
        }
    }
}