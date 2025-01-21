package com.chromia.cli.command.node

import com.chromia.build.tools.compile.withSigner
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.StorageBuilder
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.mapper.toObject

class UpdateCommand(
) : AbstractNodeCommand(help = """
    Updates a running test node
    
    Will add a configuration to a block height 2 higher than current height for the running blockchain. 
    Make sure this command is executed with exactly the same chromia.yml and arguments as was used when starting the node using `chr node start` to make sure configurations are added to the correct chain ids.
""".trimIndent()) {

    private val preemption by option("-n", "--preemption", help = "Update the configuration at a height this many blocks into the future")
            .int().default(2)
            .validate { require(it > 1) { "Must be more than one block in the future" } }

    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, wipeDatabase = false)

        extractConfigs().toList().forEachIndexed { index, (_, gtv) ->
            val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
            withReadWriteConnection(storage, index.toLong()) { eContext: EContext ->
                //TODO move to postchain
                val previousBlockchainRids = BlockchainApi.listConfigurations(eContext)
                        .map { BlockchainApi.getConfiguration(eContext, it)!! }
                        .map { GtvToBlockchainRidFactory.calculateBlockchainRid(GtvDecoder.decodeGtv(it).toObject()) }
                val blockchainRid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners.toObject())
                val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                if (lastHeight < 0) throw PrintMessage("Blockchain must be initialized before you can update it")
                if (previousBlockchainRids.contains(blockchainRid)) throw PrintMessage("Blockchain configuration already exists in database, cannot update")
                BlockchainApi.addConfiguration(eContext, lastHeight + preemption, override = true, gtvWithSigners, allowUnknownSigners = true)
                echo("Configuration added at height ${lastHeight + preemption}")
            }
        }
    }
}
