package com.chromia.cli

import com.chromia.cli.compile.NodeConfig.getDefaultNodeConfig
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.sourceDirOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
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
    private val sourceDir by sourceDirOption()
    private val settings by settingsOption()
    private val nodeConfig by nodePropertiesOption().defaultLazy { getDefaultNodeConfig(settings) }
    //TODO: add wipe feature private val wipe by wipeDatabaseOption()

    override fun run() {
        startPostchainNode()
    }
    private fun startPostchainNode() {
        val sourceDir = C_SourceDir.diskDir(sourceDir)
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeConfig, true) // TODO: add wipe feature
        BlockchainConfigurationGenerator(settings, sourceDir, nodeConfig).generate().toList().forEachIndexed { index, (namedBlockchain, gtv) ->
            val iid = index + 100L
            chainsToStart.add(iid)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeConfig.pubKey,
                    CHAIN_IID_TAG to iid.toString(),
                    BLOCKCHAIN_RID_TAG to namedBlockchain.blockchainRid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, iid) { eContext: EContext ->
                    //TODO: If not wipe -> BlockchainApi.getLastBlockHeight()
                    // TODO: If not wipe -> BlockchainApi.addConfiguration()
                    BlockchainApi.initializeBlockchain(eContext, namedBlockchain.blockchainRid, override = true, gtv)
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
