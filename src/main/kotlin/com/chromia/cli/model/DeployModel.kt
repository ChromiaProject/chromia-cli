package com.chromia.cli.model

import net.postchain.common.BlockchainRid

data class DeploymentModel(
        private val brid: String,
        var licence: String?,
        var apiUrl: String,
        var containerName: String
) {
    val blockchainRid: BlockchainRid = BlockchainRid.buildFromHex(brid)
}