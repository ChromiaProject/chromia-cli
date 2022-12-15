package com.chromia.cli.model

import net.postchain.common.BlockchainRid
import net.postchain.common.types.WrappedByteArray

data class DeploymentModel(
        private val brid: WrappedByteArray,
        val container: String?, // Container id
        val apiUrl: List<String>,
        val chains: Map<String, BlockchainRid> = mapOf()
) {
    val blockchainRid: BlockchainRid = BlockchainRid(brid)
}