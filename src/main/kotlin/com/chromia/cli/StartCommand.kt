package com.chromia.cli

import com.chromia.cli.util.wipeDatabaseOption
import com.chromia.cli.util.withSigner
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext
import net.postchain.logging.BLOCKCHAIN_RID_TAG
import net.postchain.logging.CHAIN_IID_TAG
import net.postchain.logging.NODE_PUBKEY_TAG

class StartCommand : AbstractNodeCommand(help = """
    Starts a test node
    
    If a blockchain has already been started on the configured database schema, the configuration will be added to the next height such that the node will be started with the new config. 
    Use --wipe to wipe the database schema upon startup and thus enforce starting the chain from height=0.
""".trimIndent()) {
    private val wipe by wipeDatabaseOption()


    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }
    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeConfig, wipeDb = wipe)

        extractConfigs().toList().forEachIndexed { index, (name, brid, gtv) ->
            val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
            val iid = index.toLong()
            chainsToStart.add(iid)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeConfig.pubKey,
                    CHAIN_IID_TAG to iid.toString(),
                    BLOCKCHAIN_RID_TAG to brid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, iid) { eContext: EContext ->
                    BlockchainApi.initializeBlockchain(eContext, brid, override = true, gtvWithSigners)
                    val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                    if (lastHeight >= 0) {
                        BlockchainApi.addConfiguration(eContext, lastHeight + 1 , override = true, gtvWithSigners)
                    }
                }
            }
        }
        chainsToStart.forEach {
            node.startBlockchain(it)
        }
    }
}
