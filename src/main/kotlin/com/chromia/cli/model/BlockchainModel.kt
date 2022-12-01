package com.chromia.cli.model

import net.postchain.gtv.Gtv

data class BlockchainModel(
        val module: String,
        val moduleArgs: Map<String, Map<String,Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val blockchainStrategy: Map<String, Gtv> = mapOf()
) {
}
