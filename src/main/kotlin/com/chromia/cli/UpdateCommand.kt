package com.chromia.cli

import net.postchain.StorageBuilder
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext

class UpdateCommand : AbstractNodeCommand(help = "Updates a node") {
    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, false)

        extractConfigs().toList().forEachIndexed { index, (_, gtv) ->
            val gtvWithSigners = addSigners(gtv)
            withReadWriteConnection(storage, index.toLong()) { eContext: EContext ->
                val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                require(lastHeight >= 0) { "Blockchain must be initialized before you can update it, $lastHeight" }
                BlockchainApi.addConfiguration(eContext, lastHeight + 5, override = true, gtvWithSigners, allowUnknownSigners = true)
                echo("Configuration added at height ${lastHeight + 5}")
            }
        }
    }
}
