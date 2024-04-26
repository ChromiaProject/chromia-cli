package com.chromia.build.tools.restapi

import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.TxRid
import net.postchain.gtx.Gtx

interface CachedModel : Model {
    val txQueue: List<Gtx>
    val txMap: Map<TxRid, Gtx>
}
