package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import java.io.File
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.GtvNull
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class SuccessfulDeploymentActionModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}
    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "get_compressed_configuration_parts" -> GtvFactory.gtv(listOf())
            else -> GtvNull
        }
    }
}


class DeployActionIT {

    @Test
    fun deploymentRemoveSuccessful() {
        withModel(SuccessfulDeploymentActionModel(BlockchainRid.ZERO_RID)) {
            val lines = TestProcess.Builder("deployment", "remove", "--network", "test", "--blockchain", "deployed", "--secret", secretFile.absolutePath)
                    .setConfig(settingsFile)
                    .start { it.readLines() }
            assertThat(lines[0]).contains("remove of blockchain \"deployed\" was successful")
            assertThat(lines[1]).contains("INFO: Clean up deployment \"deployed\" from your config file under chains for network \"test\", as it is no longer a valid deployment")
        }

    }

    @Test
    fun deploymentPauseSuccessful() {
        withModel(SuccessfulDeploymentActionModel(BlockchainRid.ZERO_RID)) {
            val lines = TestProcess.Builder("deployment", "pause", "--network", "test", "--blockchain", "deployed", "--secret", secretFile.absolutePath)
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .start { it.readLines() }
            assertThat(lines[0]).contains("pause of blockchain \"deployed\" was successful")
        }
    }

    @Test
    fun deploymentResumeSuccessful() {
        withModel(SuccessfulDeploymentActionModel(BlockchainRid.ZERO_RID)) {
            val lines = TestProcess.Builder("deployment", "resume", "--network", "test", "--blockchain", "deployed", "--secret", secretFile.absolutePath)
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .start { it.readLines() }
            assertThat(lines[0]).contains("resume of blockchain \"deployed\" was successful")
        }
    }

    companion object {
        @TempDir
        private lateinit var dir: Path
        private lateinit var secretFile: File
        private lateinit var settingsFile: File

        @JvmStatic
        @BeforeAll
        fun setup() {
            testData(dir)
            settingsFile = File(dir.toFile(), "chromia.yml")
            with(settingsFile) {
                appendText("\n")
                appendText("""
                blockchains:
                  deployed:
                    module: main      
                deployments:
                  test:
                    url: "$apiUrl"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
                    chains:
                      deployed: x"0000000000000000000000000000000000000000000000000000000000000002"
            """.trimIndent())
            }
            secretFile = File(dir.toFile(), ".secret")
            with(secretFile) {
                writeText("""
                pubkey=039B9ED551D5BDCC52FF9418ED77FBA7D761B24B7D06596829771A6DEA50E613AD
                privkey=D33345577D6E08997D35D3D359DAF6CD4AF91651B2C006F9974E4B73E06574F7
            """.trimIndent())
            }
        }
    }
}
