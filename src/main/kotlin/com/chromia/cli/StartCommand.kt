package com.chromia.cli

import com.chromia.cli.compile.NodeConfig.getDefaultNodeConfig
import com.chromia.cli.compile.config.NamedBlockchainRid
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.wipeDatabaseOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
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
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.metrics.BLOCKCHAIN_RID_TAG
import net.postchain.metrics.CHAIN_IID_TAG
import net.postchain.metrics.NODE_PUBKEY_TAG
import net.postchain.rell.utils.PostchainUtils

class StartCommand : CliktCommand(help = "Starts a node") {
    private val settings by settingsOption()
    private val wipe by wipeDatabaseOption()
    private val blockchainConfigs by option("-bc", "--blockchain-config",
            help = "Manually specify which blockchain-configs to run")
            .file(mustExist = true, canBeDir = false)
            .multiple()

    private val name by option(help = "Only start specified blockchains (multiple)", metavar = "NAME")
            .multiple()

    private val nodeConfig by nodePropertiesOption().defaultLazy { getDefaultNodeConfig(settings) }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }
    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Long>()
        val node = PostchainNode(nodeConfig, wipeDb = wipe, debug = true)
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            BuildCommand.compile(settings)
        } else {
            blockchainConfigs
                    .associate {
                        if (it.extension == "gtv") {
                            it.nameWithoutExtension to GtvDecoder.decodeGtv(it.inputStream())
                        }else {
                            it.nameWithoutExtension to PostchainUtils.xmlToGtv(it.readText())
                        }
                    }
                    .mapKeys { NamedBlockchainRid(it.key, BlockchainRid(PostchainUtils.calcBlockchainRid(it.value).toByteArray())) }
        }

        val configsToStart = if (name.isEmpty()) configsToAdd else configsToAdd.filter { name.contains(it.key.name)  }

        configsToStart.toList().forEachIndexed { index, (namedBlockchain, gtv) ->
            val gtvConfig = if (gtv["signers"] == null) {
                gtv(*gtv.asDict().toList().toTypedArray(), "signers" to gtv(listOf(gtv(nodeConfig.pubKeyByteArray))))
            } else {
                gtv
            }
            val iid = index.toLong()
            chainsToStart.add(iid)
            withLoggingContext(
                    NODE_PUBKEY_TAG to nodeConfig.pubKey,
                    CHAIN_IID_TAG to iid.toString(),
                    BLOCKCHAIN_RID_TAG to namedBlockchain.blockchainRid.toHex()
            ) {
                withReadWriteConnection(node.postchainContext.storage, iid) { eContext: EContext ->
                    BlockchainApi.initializeBlockchain(eContext, namedBlockchain.blockchainRid, override = true, gtvConfig)
                    val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                    if (lastHeight > 0) {
                        BlockchainApi.addConfiguration(eContext, lastHeight + 1 , override = true, gtvConfig)
                    }
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
