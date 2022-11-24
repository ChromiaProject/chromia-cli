package com.chromia.cli.model

import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray

data class DeploymentModel(
        private val brid: WrappedByteArray,
        var licence: String?,
        var apiUrl: String,
) {
    val blockchainRid: BlockchainRid = BlockchainRid(brid)
}