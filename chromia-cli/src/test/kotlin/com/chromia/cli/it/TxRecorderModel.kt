package com.chromia.cli.it

import com.chromia.build.tools.restapi.TestModel
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtx.GtxQuery

class TxRecorderModel(val model: Model, private val responses: Map<String, Gtv> = mapOf()) : Model by model {
    constructor(blockchainRid: BlockchainRid, responses: Map<String, Gtv> = mapOf()) : this(TestModel(blockchainRid), responses)

    val txList = mutableListOf<ByteArray>()

    override fun postTransaction(tx: ByteArray) {
        txList.add(tx)
    }

    override fun query(query: GtxQuery): Gtv {
        return responses[query.name] ?: throw UserMistake("Query ${query.name} in TxRecorderModel")
    }
}