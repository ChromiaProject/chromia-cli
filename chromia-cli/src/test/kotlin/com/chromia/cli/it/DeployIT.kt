package com.chromia.cli.it

import com.chromia.build.tools.*
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables


class SuccessfulDeploymentModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> = query(query) to 0

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(9)
            "find_blockchain_rid" -> gtv("4D6232FF8DDA05FFFA66FF58C5E0DC652D165D071D8241A2FF6F85B3199EE6BC".hexStringToByteArray())
            "rell.get_rell_version" -> gtv(DefaultChromiaModelRellVersion)
            "get_container_data" -> gtv(getContainerDataResult("name"))
            "get_cluster_api_urls" -> gtv(listOf(gtv("http://localhost:7745")))
            "get_compressed_configuration_parts" -> gtv(listOf())
            "cm_get_blockchain_api_urls" -> gtv(listOf(gtv("http://localhost:7745")))
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }
}

class DeployIT {
    @Test
    fun deploymentSuccessful(@TempDir dir: Path) {
        val secretFile = File(dir.toFile(), ".secret")
        testData(dir) {
            config {
                addDeploymentsConfig()
            }
            secret {
                secretFile(dir)
            }
        }
        withModel(SuccessfulDeploymentModel(BlockchainRid.ZERO_RID)) {
            TestProcess.Builder(
                    "deployment", "create",
                    "--network", "test",
                    "--blockchain", "hello",
                    "-y",
                    "--secret", secretFile.absolutePath
            )
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .startCondition("Deployment of blockchain hello was successful")
                    .start()
        }
    }

    @Test
    fun deploymentSuccessfulUsingKeyIdFromConfig(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
            config {
                addDeploymentsConfig()
            }
            secret()
        }
        val config = dir.resolve("config").absolutePathString()

        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            withModel(SuccessfulDeploymentModel(BlockchainRid.ZERO_RID)) {
                TestProcess.Builder(
                        "deployment", "create",
                        "--network", "test",
                        "--blockchain", "hello",
                        "-y", "--config", config
                )
                        .setConfig(dir.resolve("chromia.yml").toFile())
                        .startCondition("Deployment of blockchain hello was successful")
                        .start()
            }
        }
    }

    @Test
    fun deploymentWithCustomGtxModule(@TempDir dir: Path) {
        val secretFile = File(dir.toFile(), ".secret")
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        config:
                          blockstrategy:
                            maxblocktime: 1000
                          gtx:
                            modules:
                              - "net.postchain.CustomGTXModule"
                """.trimIndent())
            }
            config {
                addDeploymentsConfig()
            }
            secret {
                secretFile(dir)
            }
        }

        withModel(SuccessfulDeploymentModel(BlockchainRid.ZERO_RID)) {
            TestProcess.Builder(
                    "deployment", "create",
                    "--network", "test",
                    "--blockchain", "hello",
                    "-y",
                    "--secret", secretFile.absolutePath
            )
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .startCondition("Deployment of blockchain hello was successful")
                    .start()
        }
    }
}