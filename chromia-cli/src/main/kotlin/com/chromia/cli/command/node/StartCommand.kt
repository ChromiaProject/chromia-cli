package com.chromia.cli.command.node

import com.chromia.build.tools.compile.withSigner
import com.chromia.cli.util.logSqlOption
import com.chromia.cli.util.wipeDatabaseOption
import com.github.ajalt.clikt.core.PrintMessage
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.StorageInitializer
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.runStorageCommand
import net.postchain.base.withReadWriteConnection
import net.postchain.common.BlockchainRid
import net.postchain.core.EContext
import net.postchain.gtv.mapper.toObject
import net.postchain.logging.BLOCKCHAIN_RID_TAG
import net.postchain.logging.CHAIN_IID_TAG
import net.postchain.logging.NODE_PUBKEY_TAG
import net.postchain.rell.module.RellPostchainModuleEnvironment
import com.github.ajalt.mordant.rendering.TextColors

const val INITILIZED_LOG = "Node is initialized"

class StartCommand : AbstractNodeCommand(help = """
    Starts a test node
    
    If a blockchain has already been started on the configured database schema, the configuration will be added to the next height such that the node will be started with the new config. 
    Use --wipe to wipe the database schema upon startup and thus enforce starting the chain from height=0.
""".trimIndent()) {
    private val sqlLog by logSqlOption()
    private val wipe by wipeDatabaseOption()

    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Pair<String, Long>>()
        val environment = RellPostchainModuleEnvironment(
                sqlLog = sqlLog
        )
        RellPostchainModuleEnvironment.set(environment) {
            echo("Starting node with pubkey: ${nodeConfig.pubKey}")
            val node = PostchainNode(nodeConfig, wipeDb = wipe)

            extractConfigs().toList().forEachIndexed { index, (name, gtv) ->
                val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
                val brid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners.toObject())
                val iid = index.toLong()
                chainsToStart.add(name to iid)
                withReadWriteConnection(node.postchainContext.sharedStorage, iid) { eContext: EContext ->
                    if (BlockchainApi.findBlockchain(eContext) == null) {
                        BlockchainApi.initializeBlockchain(eContext, brid, override = true, gtvWithSigners)
                    }

                    val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                    if (lastHeight >= 0) {

                        val previousBlockchainRids = BlockchainApi.listConfigurationHashes(eContext)
                                .map { BlockchainRid(it) }
                                .toMutableList()

                        val lastBlockchainRid = previousBlockchainRids.removeAt(previousBlockchainRids.lastIndex)
                        val blockchainRid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners.toObject())

                        if (previousBlockchainRids.contains(blockchainRid)) throw PrintMessage("Blockchain configuration already exists in database, cannot start on already used config")
                        if (blockchainRid != lastBlockchainRid) BlockchainApi.addConfiguration(eContext, lastHeight + 1, override = true, gtvWithSigners)
                    }
                }
            }
            runStorageCommand(nodeConfig) {
                StorageInitializer.setupInitialPeers(nodeConfig, it)
            }
            chainsToStart.forEach {
                val startedBrid = node.startBlockchain(it.second)
                echo("Started blockchain ${TextColors.blue(it.first)}. Chain-id: ${it.second}, brid $startedBrid")
            }
            echo(INITILIZED_LOG)
        }
    }
}
