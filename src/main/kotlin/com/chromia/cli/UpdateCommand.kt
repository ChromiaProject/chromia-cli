package com.chromia.cli

import com.chromia.cli.util.withSigner
import net.postchain.StorageBuilder
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext

class UpdateCommand : AbstractNodeCommand(help = """
    Updates a running test node
    
    Will add a configuration to a block height 5 higher that current height for the running blockchain. 
    Make sure this command is executed with exactly the same config.yml and arguments as was used when starting the node using `chr node start` to make sure configurations are added to the correct chain ids.
""".trimIndent()) {
    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, false)

        extractConfigs().toList().forEachIndexed { index, (_, gtv) ->
            val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
            withReadWriteConnection(storage, index.toLong()) { eContext: EContext ->
                val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                require(lastHeight >= 0) { "Blockchain must be initialized before you can update it, $lastHeight" }
                BlockchainApi.addConfiguration(eContext, lastHeight + 5, override = true, gtvWithSigners, allowUnknownSigners = true)
                echo("Configuration added at height ${lastHeight + 5}")
            }
        }
    }
}
