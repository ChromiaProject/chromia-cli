package com.chromia.cli.model

import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv

data class BlockchainModel(
        val module: String,
        val moduleArgs: Map<String, Map<String, Gtv>> = mapOf(),
        val config: Map<String, Gtv> = mapOf(),
        val libs: Map<String, InstallAttribute> = mapOf(),
)

data class InstallAttribute(
        val registry: String,
        val lib: String,
        val rid: WrappedByteArray?,
)