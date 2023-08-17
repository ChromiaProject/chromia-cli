package com.chromia.cli.it

import com.chromia.build.tools.TestModelImpl
import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.nio.file.Path
import java.time.Duration
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.Gtx
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class TxDeploymentModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModelImpl(blockchainRid))

    val txList = mutableListOf<ByteArray>()

    override fun postTransaction(tx: ByteArray) {
        txList.add(tx)
    }
}

class TxIT {

    @Test
    fun queryTowardsDeployment(@TempDir dir: Path) {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
        testData(dir) {
            config {
                deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7741"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        container: testcontainer
                        chains:
                          hello: x"${testBrid.toHex()}"
                        """.trimIndent()
                )
            }
            secret()
        }
        val txDeploymentModel = TxDeploymentModel(testBrid)
        RestApi(7741, "").use { api ->
            api.attachModel(BlockchainRid.ZERO_RID, Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf("http://localhost:7741"))))
            api.attachModel(testBrid, txDeploymentModel)

            TestProcess.Builder("tx", "call_op", "13", "--network", "test", "--blockchain", "hello")
                    .setWorkingDir(dir.toFile())
                    .start { process ->
                        process.waitUntil("was posted WAITING: OK", Duration.ofSeconds(5))
                    }
        }
        assertThat(txDeploymentModel.txList.size).isEqualTo(1)
        val gtx = Gtx.decode(txDeploymentModel.txList.first())
        assertThat(gtx.signatures.size).isEqualTo(0) // TODO: Should be 1 after upgrade of postchain-chromia
        assertThat(gtx.gtxBody.operations.size).isEqualTo(1)
        val op = gtx.gtxBody.operations.first()
        assertThat(op.opName).isEqualTo("call_op")
        assertThat(op.args).containsExactly(gtv(13))
    }
}
