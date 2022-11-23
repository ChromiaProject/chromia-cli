package com.chromia.cli.model

import net.postchain.common.BlockchainRid

data class DeploymentModel(val brid: String, var licence: String?, var apiUrl: String, var containerName: String) {
    val bridBinary: BlockchainRid = BlockchainRid.buildFromHex(brid)
}