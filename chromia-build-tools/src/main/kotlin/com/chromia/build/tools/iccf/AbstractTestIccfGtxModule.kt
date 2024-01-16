package com.chromia.build.tools.iccf

import net.postchain.core.EContext
import net.postchain.core.Transactor
import net.postchain.gtx.SimpleGTXModule
import net.postchain.gtx.data.ExtOpData

abstract class AbstractTestIccfGtxModule<ConfT>(conf: ConfT, iccfOperation: (ConfT, ExtOpData) -> Transactor): SimpleGTXModule<ConfT>(
        conf = conf,
        opmap = mapOf("iccf_proof" to iccfOperation),
        querymap = mapOf()
) {
    override fun initializeDB(ctx: EContext) = Unit
}