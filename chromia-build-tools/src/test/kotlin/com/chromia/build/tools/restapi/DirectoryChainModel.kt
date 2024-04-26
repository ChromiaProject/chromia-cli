package com.chromia.build.tools.restapi

import com.chromia.directory1.common.queries.ContainerData
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery

class DirectoryChainModel(val model: CachedModel, val apiVersion: Long) : CachedModel by model {
    constructor(apiVersion: Long = 28) : this(TestModel(BlockchainRid.ZERO_RID), apiVersion)

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(apiVersion)
            "get_container_data" -> GtvObjectMapper.toGtvDictionary(ContainerData("test_container", "test_cluster", "", "".hexStringToWrappedByteArray(), "deployer_name", false, null))
            "get_cluster_api_urls" -> gtv(gtv(RestApiInstance.apiUrl))
            else -> model.query(query)
        }
    }
}
