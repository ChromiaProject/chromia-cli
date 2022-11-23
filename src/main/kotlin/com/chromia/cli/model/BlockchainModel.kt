package com.chromia.cli.model

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
data class BlockchainModel(
        val module: String,
        val moduleArgs: Map<String, Map<String,Gtv>> = mapOf(),
        val additionalGtv: Gtv = GtvFactory.gtv(mapOf()),
        val blockchainStrategy: Map<String, Gtv> = mapOf()
) {
}