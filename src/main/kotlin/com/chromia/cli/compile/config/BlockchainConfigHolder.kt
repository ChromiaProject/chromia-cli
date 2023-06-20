package com.chromia.cli.compile.config

import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder

data class BlockchainConfigHolder(val name: String, val config: Gtv) {
    val configByteArray get() = GtvEncoder.encodeGtv(config)
}
