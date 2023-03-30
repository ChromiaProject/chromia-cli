package com.chromia.cli

import com.chromia.cli.util.withSigner
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.StorageBuilder
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext

class UpdateCommand : AbstractNodeCommand(help = """
    Updates a running test node
    
    Will add a configuration to a block height 5 higher that current height for the running blockchain. 
    Make sure this command is executed with exactly the same config.yml and arguments as was used when starting the node using `chr node start` to make sure configurations are added to the correct chain ids.
""".trimIndent()) {

    private val preemption by option("-n", "--preemption", help = "Update the configuration at a height this many blocks into the future")
            .int().default(2)
            .validate { require(it > 1) { "Must be more than one block in the future" } }
    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, false)

        extractConfigs().toList().forEachIndexed { index, (_, _, gtv) ->
            val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
            withReadWriteConnection(storage, index.toLong()) { eContext: EContext ->
                val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                require(lastHeight >= 0) { "Blockchain must be initialized before you can update it, $lastHeight" }
                BlockchainApi.addConfiguration(eContext, lastHeight + preemption, override = true, gtvWithSigners, allowUnknownSigners = true)
                echo("Configuration added at height ${lastHeight + preemption}")
            }
        }
    }
}
