package com.chromia.cli.it

import com.chromia.build.tools.RestApiInstance.apiUrl
import com.chromia.build.tools.RestApiInstance.withModel
import com.chromia.build.tools.TestModel
import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.nio.file.Path
import java.time.Duration
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.Gtx
import net.postchain.gtx.GtxQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class TxRecorderModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    val txList = mutableListOf<ByteArray>()

    override fun postTransaction(tx: ByteArray) {
        txList.add(tx)
    }
}

class Ft4Model(val model: Model, val version: String, val responses: Map<String, Gtv> = mapOf()) : Model by model {
    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "ft4.get_version" -> gtv(version)
            else -> responses[query.name] ?: throw UserMistake("Query ${query.name} not found")
        }
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
                        url: "$apiUrl"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        container: testcontainer
                        chains:
                          hello: x"${testBrid.toHex()}"
                        """.trimIndent()
                )
            }
            secret()
        }
        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(
                Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf(apiUrl))),
                txRecorderModel
        ) {

            TestProcess.Builder("tx", "call_op", "13", "--network", "test", "--blockchain", "hello")
                    .setWorkingDir(dir.toFile())
                    .start { process ->
                        process.waitUntil("was posted WAITING: OK", Duration.ofSeconds(5))
                    }
        }
        assertThat(txRecorderModel.txList.size).isEqualTo(1)
        val gtx = Gtx.decode(txRecorderModel.txList.first())
        assertThat(gtx.signatures.size).isEqualTo(0) // TODO: Should be 1 after upgrade of postchain-chromia
        assertThat(gtx.gtxBody.operations.size).isEqualTo(1)
        val op = gtx.gtxBody.operations.first()
        assertThat(op.opName).isEqualTo("call_op")
        assertThat(op.args).containsExactly(gtv(13))
    }

    @Test
    fun queryWithOldFtAuthFails(@TempDir dir: Path) {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
        testData(dir) {
            secret()
        }

        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(Ft4Model(txRecorderModel, "0.1.0r")) {
            TestProcess.Builder("tx", "--api-url", apiUrl, "call_op", "13", "--ft-auth")
                    .setWorkingDir(dir.toFile())
                    .awaitCompletion(false)
                    .start { process ->
                        process.waitUntil("FT version 0.1.0r not supported", Duration.ofSeconds(5))
                        process.close()
                        assertThat(process.process.exitValue()).isEqualTo(1)
                    }
        }
    }

    @Test
    fun queryWithFtAuth(@TempDir dir: Path) {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
        testData(dir) {
            secret()
        }

        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(Ft4Model(
                txRecorderModel,
                "0.1.0",
                mapOf(
                        "ft4.get_accounts_by_participant_id" to gtv(gtv("1".repeat(64).hexStringToByteArray())),
                        "ft4.get_account_auth_descriptors_by_participant_id" to gtv(gtv(mapOf(
                                "id" to gtv("2".repeat(64).hexStringToByteArray()),
                                "args" to gtv(gtv(gtv("A")), gtv(testBrid)),
                                "created" to gtv(System.currentTimeMillis()),
                                "auth_type" to gtv("A"),
                                "rules" to GtvNull
                        ))
                        ),
                        "ft4.get_auth_flags" to gtv(gtv("A"))
                )
        )) {
            TestProcess.Builder("tx", "--api-url", apiUrl, "call_op", "13", "--ft-auth")
                    .verbose()
                    .setWorkingDir(dir.toFile())
                    .start()

            val gtx = Gtx.decode(txRecorderModel.txList.single())
            val operations = gtx.gtxBody.operations
            assertThat(operations[0].opName).isEqualTo("ft4.ft_auth")
            assertThat(operations[0].args).containsExactly(gtv("1".repeat(64).hexStringToByteArray()), gtv("2".repeat(64).hexStringToByteArray()))
            assertThat(operations[1].opName).isEqualTo("call_op")
        }
    }
}
