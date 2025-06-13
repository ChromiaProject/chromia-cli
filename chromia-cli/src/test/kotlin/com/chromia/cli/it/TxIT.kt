package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import com.chromia.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.lib.ft4.version.GET_VERSION
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.Gtx
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.absolutePathString


class AlwaysFailingModel(val model: Model, val status: TransactionStatus) : Model by model {
    constructor(blockchainRid: BlockchainRid, status: TransactionStatus) : this(TestModel(blockchainRid), status)


    override fun postTransaction(tx: ByteArray) = Unit
    override fun getStatus(txRID: TxRid) = ApiStatus(status)
}

class Ft4Model(val model: Model, val version: String, val responses: Map<String, Gtv> = mapOf()) : Model by model {
    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            GET_VERSION -> gtv(version)
            else -> responses[query.name] ?: throw UserMistake("Query ${query.name} not found in Ft4Model")
        }
    }
}

class TxIT {

    companion object {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
    }

    @Test
    fun formatHelp() {
        TestProcess.Builder("tx", "--help")
                .exitCode(0)
                .timeout(Duration.ofSeconds(10))
                .startCondition("Make a transaction towards a node")
                .start()
    }

    @Test
    fun queryTowardsDeployment(@TempDir dir: Path) {
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

            TestProcess.Builder("tx", "call_op", "13", "--network", "test", "--blockchain", "hello", "--no-await")
                    .setWorkingDir(dir.toFile())
                    .start { process ->
                        process.waitUntil("was posted but is still pending", Duration.ofSeconds(5))
                    }
        }
        assertThat(txRecorderModel.txList.size).isEqualTo(1)
        val gtx = Gtx.decode(txRecorderModel.txList.first())
        assertThat(gtx.signatures.size).isEqualTo(1)
        assertThat(gtx.gtxBody.operations.size).isEqualTo(1)
        val op = gtx.gtxBody.operations.first()
        assertThat(op.opName).isEqualTo("call_op")
        assertThat(op.args).containsExactly(gtv(13))
    }

    @Test
    fun transactionUsingKeyIdFromConfigFile(@TempDir dir: Path) {
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
            keyStore()
        }

        val config = dir.resolve("config").absolutePathString()
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            val txRecorderModel = TxRecorderModel(testBrid)
            withModel(
                    Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf(apiUrl))),
                    txRecorderModel
            ) {

                TestProcess.Builder("tx", "call_op", "13", "--network", "test", "--blockchain", "hello", "--config", config, "--no-await")
                        .setWorkingDir(dir.toFile())
                        .start { process ->
                            process.waitUntil("was posted but is still pending", Duration.ofSeconds(5))
                        }
            }
            assertThat(txRecorderModel.txList.size).isEqualTo(1)
            val gtx = Gtx.decode(txRecorderModel.txList.first())
            assertThat(gtx.signatures.size).isEqualTo(1)
            assertThat(gtx.gtxBody.operations.size).isEqualTo(1)
            val op = gtx.gtxBody.operations.first()
            assertThat(op.opName).isEqualTo("call_op")
            assertThat(op.args).containsExactly(gtv(13))
        }
    }

    @Test
    fun queryWithOldFtAuthFails(@TempDir dir: Path) {
        testData(dir) {
            secret()
        }

        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(Ft4Model(txRecorderModel, "0.1.0r")) {
            TestProcess.Builder("tx", "--api-url", apiUrl, "call_op", "13", "--ft-auth")
                    .setWorkingDir(dir.toFile())
                    .awaitCompletion(false)
                    .start { process ->
                        process.waitUntil("Versions before release 0.4.0 are not supported, current FT4 version 0.1.0r is to old", Duration.ofSeconds(10))
                        process.close()
                        assertThat(process.process.exitValue()).isEqualTo(1)
                    }
        }
    }


    @Test
    fun queryWithFtAuthV4(@TempDir dir: Path) {
        testData(dir) {
            secret()
        }
        val pubKey = TestDataBuilder.keyPair.pubKey

        val txRecorderModel = TxRecorderModel(testBrid)
        withModel(Ft4Model(
                txRecorderModel,
                "0.4.0",
                mapOf(
                        GET_ACCOUNTS_BY_SIGNER to gtv(mapOf("data" to gtv(gtv(mapOf("id" to gtv("3".repeat(64).hexStringToByteArray())))))),
                        GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER to gtv(gtv(mapOf(
                                "id" to gtv("4".repeat(64).hexStringToByteArray()),
                                "args" to gtv(gtv(gtv("A")), gtv(pubKey.data)),
                                "created" to gtv(System.currentTimeMillis()),
                                "auth_type" to gtv("S"),
                                "rules" to GtvNull,
                                "account_id" to gtv("5".repeat(64).hexStringToByteArray())

                        ))),
                        GET_AUTH_FLAGS to gtv(gtv("A"))
                )
        )) {
            TestProcess.Builder("tx", "--api-url", apiUrl, "call_op", "13", "--ft-auth", "--no-await")
                    .setWorkingDir(dir.toFile())
                    .start()

            val gtx = Gtx.decode(txRecorderModel.txList.single())
            val operations = gtx.gtxBody.operations
            assertThat(operations[0].opName).isEqualTo("ft4.ft_auth")
            assertThat(operations[0].args).containsExactly(gtv("3".repeat(64).hexStringToByteArray()), gtv("4".repeat(64).hexStringToByteArray()))
            assertThat(operations[1].opName).isEqualTo("call_op")
        }
    }

    @Test
    fun failingTxExitCode(@TempDir dir: Path) {
        testData(dir)
        withModel(AlwaysFailingModel(testBrid, TransactionStatus.REJECTED)) {
            TestProcess.Builder("tx", "--api-url", apiUrl, "call_op", "13", "--await")
                    .exitCode(1)
                    .setWorkingDir(dir.toFile())
                    .start()

        }
    }
}
