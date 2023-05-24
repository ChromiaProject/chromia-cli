package com.chromia.cli.compile.config

import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.gtx.PostchainBaseUtils

data class BlockchainConfigHolder(val name: String, val brid: BlockchainRid, val config: Gtv) {
    val configByteArray get() = GtvEncoder.encodeGtv(config)
    companion object {
        fun from(name: String, config: Gtv): BlockchainConfigHolder {
            return BlockchainConfigHolder(name, BlockchainRid(PostchainBaseUtils.calcBlockchainRid(config).toByteArray()), config)
        }
    }
}
