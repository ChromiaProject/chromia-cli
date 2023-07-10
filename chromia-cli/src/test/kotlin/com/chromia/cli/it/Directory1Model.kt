package com.chromia.cli.it

import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.GtxQuery

class Directory1Model(val model: Model, val chains: Map<BlockchainRid, List<String>>) : Model by model {
    constructor(blockchainRid: BlockchainRid, chains: Map<BlockchainRid, List<String>>) : this(TestModelImpl(blockchainRid), chains)
    override fun query(query: GtxQuery): Gtv {
        require(query.name == "cm_get_blockchain_api_urls")
        val brid = BlockchainRid(query.args.asDict()["blockchain_rid"]!!.asByteArray())
        return GtvFactory.gtv(chains[brid]!!.map { GtvFactory.gtv(it) })
    }
}