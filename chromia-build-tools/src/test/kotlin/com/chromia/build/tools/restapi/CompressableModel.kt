package com.chromia.build.tools.restapi

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery

class CompressableModel(val model: CachedModel) : CachedModel by model {
    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(28)
            "get_compressed_configuration_parts" -> query.args["configuration_part_hashes"]!!
            else -> model.query(query)
        }
    }
}
