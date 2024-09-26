package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DeployInspectLeaseIT {

    class EconomyChainModel(val model: Model, val leasesByAccount: Map<WrappedByteArray, Gtv>, val leasesByContainerName: Map<String, Gtv>) : Model by model {
        constructor(blockchainRid: BlockchainRid, leasesByAccount: Map<WrappedByteArray, Gtv>, leasesByContainerName: Map<String, Gtv>) : this(TestModel(blockchainRid), leasesByAccount, leasesByContainerName)

        override fun query(query: GtxQuery): Gtv {
            return when (query.name) {
                "get_leases_by_account" -> leasesByAccount[query.args["account_id"]?.asByteArray()?.wrap()]
                        ?: throw IllegalArgumentException("Couldn't find lease by account: ${query.args["account_id"]}")

                "get_lease_by_container_name" -> leasesByContainerName[query.args["container_name"]?.asString()]
                        ?: throw IllegalArgumentException("Couldn't find lease by container name: ${query.args["container_name"]}")

                else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
            }
        }
    }

    class EconomyChainInDirectory1Model(val model: Model, val economyChainBrid: BlockchainRid) : Model by model {
        constructor(blockchainRid: BlockchainRid, economyChainBrid: BlockchainRid) : this(TestModel(blockchainRid), economyChainBrid)

        override fun query(query: GtxQuery): Gtv {
            require(query.name == "get_economy_chain_rid") { "Expected query: get_economy_chain_rid, received: ${query.name}" }
            return gtv(economyChainBrid)
        }
    }

    @Test
    fun getLeaseData(@TempDir dir: Path) {
        val d1Brid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
        val economyChainBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000002".hexStringToByteArray())
        val accountId = "0000000000000000000000000000000000000000000000000000000000000003"
        val containerName = "containerName"
        val clusterName = "clusterName"
        val containerUnits = 2L
        val extraStorageGib = 4L
        val expireTimeMillis = 6L
        val expired = false
        val autoRenew = true

        val lease = mapOf(
                "container_name" to gtv(containerName),
                "cluster_name" to gtv(clusterName),
                "container_units" to gtv(containerUnits),
                "extra_storage_gib" to gtv(extraStorageGib),
                "expire_time_millis" to gtv(expireTimeMillis),
                "expired" to gtv(expired),
                "auto_renew" to gtv(autoRenew),
                "subnode_image_name" to gtv("")
        )

        val leaseByAccountId = mapOf(accountId.hexStringToWrappedByteArray() to gtv(listOf(gtv(lease))))
        val leaseByContainerName = mapOf(containerName to gtv(lease))

        testData(dir) {
            config {
                deployments(
                        """
                deployments:
                  test:
                    url: "${RestApiInstance.apiUrl}"
                    brid: x"${d1Brid.toHex()}"
                    container: $containerName
            """.trimIndent())
            }
        }

        RestApiInstance.withModel(
                EconomyChainInDirectory1Model(d1Brid, economyChainBrid),
                EconomyChainModel(economyChainBrid, leaseByAccountId, leaseByContainerName),
        ) {
            TestProcess.Builder("deployment", "lease-info", "--network", "test")
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .wholeOutput("""
                        {
                          "pubkey": null,
                          "container": "containerName",
                          "leases": [
                            {
                              "Cluster": "$clusterName",
                              "Container": "$containerName",
                              "Container_units": $containerUnits,
                              "Extra_storage": $extraStorageGib,
                              "Expire_time": $expireTimeMillis,
                              "Expired": $expired,
                              "Auto_renew": $autoRenew
                            }
                          ]
                        }
                    """.trimIndent())
                    .start()

            TestProcess.Builder("deployment", "lease-info", "--network", "test", "--pubkey", accountId)
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .wholeOutput("""
                        {
                          "pubkey": "$accountId",
                          "container": null,
                          "leases": [
                            {
                              "Cluster": "$clusterName",
                              "Container": "$containerName",
                              "Container_units": $containerUnits,
                              "Extra_storage": $extraStorageGib,
                              "Expire_time": $expireTimeMillis,
                              "Expired": $expired,
                              "Auto_renew": $autoRenew
                            }
                          ]
                        }
                    """.trimIndent())
                    .start()
        }
    }
}
