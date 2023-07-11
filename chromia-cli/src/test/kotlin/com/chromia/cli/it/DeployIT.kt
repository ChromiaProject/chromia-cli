package com.chromia.cli.it

import com.chromia.cli.util.testData
import java.io.File
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class SuccessfulDeploymentModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModelImpl(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(9)
            "find_blockchain_rid" -> gtv("039B9ED551D5BDCC52FF9418ED77FBA7D761B24B7D06596829771A6DEA50E613".hexStringToByteArray())
            "rell.get_rell_version" -> gtv("0.13.1")
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }
}

class DeployIT {

    @Test
    fun deploymentSuccesful(@TempDir dir: Path) {
        testData(dir)
        with(File(dir.toFile(), "config.yml")) {
            appendText("\n")
            appendText("""
                deployments:
                  test:
                    url: "http://localhost:7740"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
            """.trimIndent())
        }
        val secretFile = File(dir.toFile(), ".secret")
        with(secretFile) {
            writeText("""
                pubkey=039B9ED551D5BDCC52FF9418ED77FBA7D761B24B7D06596829771A6DEA50E613AD
                privkey=D33345577D6E08997D35D3D359DAF6CD4AF91651B2C006F9974E4B73E06574F7
            """.trimIndent())
        }
        RestApi(7740, "").use {
            it.attachModel(BlockchainRid.ZERO_RID, SuccessfulDeploymentModel(BlockchainRid.ZERO_RID))

            ChrProcess.Builder("deployment", "create", "--network", "test", "--blockchain", "hello", "-y", "--secret", secretFile.absolutePath)
                    .setConfig(dir.resolve("config.yml").toFile())
                    .start { process ->
                        assertThat(process.readLines()).anyMatch { it.contains("Deployment of blockchain hello was successful") }
                    }
        }
    }
}
