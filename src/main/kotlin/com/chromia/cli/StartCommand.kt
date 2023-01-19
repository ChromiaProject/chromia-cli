package com.chromia.cli

import com.chromia.cli.util.wipeDatabaseOption
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.metrics.BLOCKCHAIN_RID_TAG
import net.postchain.metrics.CHAIN_IID_TAG
import net.postchain.metrics.NODE_PUBKEY_TAG

class StartCommand : AbstractNodeCommand(help = "Starts a node") {
    private val wipe by wipeDatabaseOption()


    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }
    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeConfig, wipeDb = wipe, debug = true)

        extractConfigs().toList().forEachIndexed { index, (namedBlockchain, gtv) ->
            val gtvWithSigners = addSigners(gtv)
            val iid = index.toLong()
            chainsToStart.add(iid)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeConfig.pubKey,
                    CHAIN_IID_TAG to iid.toString(),
                    BLOCKCHAIN_RID_TAG to namedBlockchain.blockchainRid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, iid) { eContext: EContext ->
                    BlockchainApi.initializeBlockchain(eContext, namedBlockchain.blockchainRid, override = true, gtvWithSigners)
                    val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                    if (lastHeight > 0) {
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
