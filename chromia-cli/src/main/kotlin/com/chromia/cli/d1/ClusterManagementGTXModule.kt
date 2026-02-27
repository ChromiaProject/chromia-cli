package com.chromia.cli.d1

import net.postchain.base.data.PostgreSQLDatabaseAccess
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.core.EContext
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.SimpleGTXModule

/**
 * GTXModule which responds to the cluster management api when all chains are and always will be hosted on the same node.
 * Only to be used in single node test setups.
 *
 * NOT TO BE USED IN PRODUCTION
 */
class ClusterManagementGTXModule : SimpleGTXModule<ClusterManagementGTXModule.Companion>(
        Companion,
        opmap = mapOf(),
        querymap = mapOf(
                "cm_get_cluster_info" to { _, ctx, args ->
                    if (args["name"]?.asString() != "test") throw IllegalArgumentException("Unknown cluster")
                    val brid = db.getBlockchainRid(ctx)!!
                    val pubkey = db.getPeerInfoCollection(ctx).first().pubKey
                    gtv(mapOf(
                            "name" to gtv("test"),
                            "anchoring_chain" to gtv(brid),
                            "peers" to gtv(
                                    gtv(mapOf(
                                            "pubkey" to gtv(pubkey),
                                            "api_url" to gtv("http://localhost:7740")
                                    ))
                            )
                    ))
                },
                "cm_get_blockchain_cluster" to { _, _, _ -> gtv("test") },
                "cm_get_cluster_names" to { _, _, _ -> gtv(gtv("test")) },
                "cm_get_cluster_blockchains" to { _, ctx, args ->
                    if (args["name"]?.asString() == "test") gtv(db.getChainIds(ctx).map { it.value to it.key }.sortedBy { it.first }.map { gtv(it.second) }) else gtv(listOf())
                },
                "cm_get_blockchain_api_urls" to { _, ctx, args ->
                    val ridArg = args["blockchain_rid"]!!
                    val blockchainRid = if (ridArg is GtvByteArray) BlockchainRid(ridArg.asByteArray()) else BlockchainRid(ridArg.asString().hexStringToByteArray())
                    if (db.getChainId(ctx, blockchainRid) != null) gtv(gtv("http://localhost:7740")) else gtv(listOf())
                },
                "cm_get_cluster_anchoring_chains" to { _, ctx, _ -> gtv(listOf(gtv(db.getBlockchainRid(ctx)!!))) },
                "cm_get_system_anchoring_chain" to { _, ctx, _ -> gtv(db.getBlockchainRid(ctx)!!) },
                "cm_get_system_chains" to { _, ctx, _ ->
                    db.getBlockchainRid(ctx)?.let { gtv(gtv(it)) } ?: gtv(listOf())
                },
                "cm_api_version" to {_, _,_ -> gtv(3)},
                "cm_get_active_blockchain_api_urls" to {_,_,_ -> gtv(listOf(gtv("http://localhost:7740")))}
        )
) {

    override fun initializeDB(ctx: EContext) {}

    companion object {
        private val db = PostgreSQLDatabaseAccess()
    }
}
