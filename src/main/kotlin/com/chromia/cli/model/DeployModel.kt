package com.chromia.cli.model

import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray

data class DeploymentModel(
        private val brid: WrappedByteArray,
        val licence: String?, // Container id
        val apiUrl: String,
        val chains: Map<String, BlockchainRid> = mapOf()
) {
    val blockchainRid: BlockchainRid = BlockchainRid(brid)
}