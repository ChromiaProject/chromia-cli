package com.chromia.cli.compile.config

import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.rell.utils.PostchainUtils

data class BlockchainConfigHolder(val name: String, val brid: BlockchainRid, val config: Gtv) {
    companion object {
        fun from(name: String, config: Gtv): BlockchainConfigHolder {
            return BlockchainConfigHolder(name, BlockchainRid(PostchainUtils.calcBlockchainRid(config).toByteArray()), config)
        }
    }
}
