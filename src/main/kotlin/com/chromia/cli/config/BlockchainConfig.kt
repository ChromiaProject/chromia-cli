package com.chromia.cli.config

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory

data class BlockchainConfig(
        val name: String,
        val module: String,
        val moduleArgs: Map<String, Map<String,Gtv>> = mapOf(),
        val additionalGtv: Gtv = GtvFactory.gtv(mapOf())
) {
}