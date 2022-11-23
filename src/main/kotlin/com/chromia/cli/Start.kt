package com.chromia.cli

import com.chromia.cli.compile.NodeConfig.getDefaultNodeConfig
import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.util.configFile
import com.chromia.cli.util.nodeConfigFile
import com.chromia.cli.util.sourceDirOption
import com.chromia.cli.util.wipeSql
import com.github.ajalt.clikt.core.CliktCommand
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext
import net.postchain.metrics.BLOCKCHAIN_RID_TAG
import net.postchain.metrics.CHAIN_IID_TAG
import net.postchain.metrics.NODE_PUBKEY_TAG
import net.postchain.rell.compiler.base.utils.C_SourceDir

class StartCommand: CliktCommand(help= "Starts a node"){
    private val sourceFile by sourceDirOption()
    private val nodeConfig by nodeConfigFile()
    private val config by configFile()
    private val wipe by wipeSql()

    override fun run() {
        startPostchainNode()
    }
    private fun startPostchainNode() {
        val nodeAppConf = nodeConfig?.let { getNodeConfig(it) } ?: getDefaultNodeConfig(config)
        val sourceDir = C_SourceDir.diskDir(sourceFile)
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeAppConf, wipe)
        BlockchainConfigurationGenerator(config, sourceDir, nodeAppConf).generateMaps().toList().forEachIndexed { index, (brid, gtv) ->
            val iid = index + 100L
            chainsToStart.add(iid)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeAppConf.pubKey,
                    CHAIN_IID_TAG to iid.toString(),
                    BLOCKCHAIN_RID_TAG to brid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, iid) { eContext: EContext ->
                    BlockchainApi.initializeBlockchain(eContext, brid, override = true, gtv)
                }
            }
        }
        runBlocking {
            chainsToStart.forEach {
                launch { node.startBlockchain(it) }
            }
        }
    }
}