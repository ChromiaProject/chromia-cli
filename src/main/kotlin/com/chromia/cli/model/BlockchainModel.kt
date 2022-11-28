package com.chromia.cli.model

import net.postchain.base.BaseBlockBuildingStrategyConfigurationData
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.mapper.toObject

data class BlockchainModel(
        val module: String,
        val moduleArgs: Map<String, Map<String,Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val blockchainStrategy: Map<String, Gtv> = mapOf()
) {
}
