package com.chromia.cli

import com.chromia.cli.compile.NodeConfig.getDefaultNodeConfig
import com.chromia.cli.compile.config.NamedBlockchainRid
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.nodePropertiesOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.withReadWriteConnection
import net.postchain.common.BlockchainRid
import net.postchain.core.EContext
import net.postchain.gtv.GtvDecoder
import net.postchain.metrics.BLOCKCHAIN_RID_TAG
import net.postchain.metrics.CHAIN_IID_TAG
import net.postchain.metrics.NODE_PUBKEY_TAG
import net.postchain.rell.utils.PostchainUtils

class StartCommand : CliktCommand(help = "Starts a node") {
    private val settings by settingsOption()
    private val blockchainConfigs by option("-bc", "--blockchain-config",
            help = "Manually specify which blockchain-configs to run")
            .file(mustExist = true, canBeDir = false)
            .multiple()

    private val nodeConfig by nodePropertiesOption().defaultLazy { getDefaultNodeConfig(settings) }
    //TODO: add wipe feature private val wipe by wipeDatabaseOption()

    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeConfig, wipeDb = true, debug = true) // TODO: add wipe feature
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            BuildCommand.compile(settings)
        } else {
            blockchainConfigs
                    .associate { it.nameWithoutExtension to GtvDecoder.decodeGtv(it.inputStream()) }
                    .mapKeys { NamedBlockchainRid(it.key, BlockchainRid(PostchainUtils.calcBlockchainRid(it.value).toByteArray())) }
        }

        configsToAdd.toList().forEachIndexed { index, (namedBlockchain, gtv) ->
            val iid = index.toLong()
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
