package com.chromia.cli.compile.config

import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv

data class BlockchainConfiguration(
        val specifiedBlockchainRid: BlockchainRid?,
        val generatedBlockchainRid: BlockchainRid,
        val blockchainName: String,
        val containerName: String?,
        val configuration: Gtv
)
