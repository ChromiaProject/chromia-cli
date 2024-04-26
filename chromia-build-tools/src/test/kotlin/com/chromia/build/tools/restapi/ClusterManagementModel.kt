package com.chromia.build.tools.restapi

import net.postchain.common.BlockchainRid
import net.postchain.d1.cluster.ClusterManagement
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery

class ClusterManagementModel(private val model: CachedModel, val clusterManagement: ClusterManagement) : CachedModel by model {
    constructor(clusterManagement: ClusterManagement) : this(TestModel(BlockchainRid.ZERO_RID), clusterManagement)

    override fun query(query: GtxQuery): Gtv {
        val (name, args) = query
        return when (name) {
            // TODO: Not exhaustive cm api and possibly parameterize
            "cm_get_cluster_names" -> clusterManagement.getClusterNames().map { gtv(it) }.let { gtv(it) }
            "cm_get_cluster_info" -> GtvObjectMapper.toGtvDictionary(clusterManagement.getClusterInfo(args["cluster_name"]!!.asString()))
            "cm_get_cluster_of_blockchain" -> gtv(clusterManagement.getClusterOfBlockchain(BlockchainRid(args["blockchain_rid"]!!.asByteArray())))
            "cm_cluster_anchoring_chains" -> clusterManagement.getClusterAnchoringChains().map { gtv(it) }.let { gtv(it) }
            "cm_get_blockchain_api_urls" -> gtv(gtv(RestApiInstance.apiUrl))
            "cm_get_blockchain_cluster" -> gtv("test_cluster")
            else -> model.query(query)
        }
    }
}
