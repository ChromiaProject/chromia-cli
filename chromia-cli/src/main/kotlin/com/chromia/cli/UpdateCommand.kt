package com.chromia.cli

import com.chromia.build.tools.compile.withSigner
import com.chromia.cli.compile.ConfigExtractor
import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.util.nodePropertiesOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.StorageBuilder
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.withReadWriteConnection
import net.postchain.core.EContext
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvDecoder

class UpdateCommand(
) : AbstractNodeCommand(help = """
    Updates a running test node
    
    Will add a configuration to a block height 5 higher that current height for the running blockchain. 
    Make sure this command is executed with exactly the same chromia.yml and arguments as was used when starting the node using `chr node start` to make sure configurations are added to the correct chain ids.
""".trimIndent()) {

    protected val settings by chromiaModelOption()
    private val preemption by option("-n", "--preemption", help = "Update the configuration at a height this many blocks into the future")
            .int().default(2)
            .validate { require(it > 1) { "Must be more than one block in the future" } }
    val cryptoSystem = Secp256K1CryptoSystem()
    private val nodeConfigFile by nodePropertiesOption()
    val nodeConfig by lazy { nodeConfigFile ?: NodeConfig.getDefaultNodeConfig(settings.model) }

    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, wipeDatabase = false)

        ConfigExtractor(settings.model, cliEnv()).extractConfigs(settings.projectFolder, blockchainConfigs, name).toList().forEachIndexed { index, (_, gtv) ->
            val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
            withReadWriteConnection(storage, index.toLong()) { eContext: EContext ->
                //TODO move to postchain
                val previousBlockchainRids = BlockchainApi.listConfigurations(eContext)
                        .map { BlockchainApi.getConfiguration(eContext, it)!! }
                        .map { GtvToBlockchainRidFactory.calculateBlockchainRid(GtvDecoder.decodeGtv(it), cryptoSystem) }
                val blockchainRid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners, cryptoSystem)
                val lastHeight = BlockchainApi.getLastBlockHeight(eContext)
                if (lastHeight < 0) throw PrintMessage("Blockchain must be initialized before you can update it")
                if (previousBlockchainRids.contains(blockchainRid)) throw PrintMessage("Blockchain configuration already exists in database, cannot update")
                BlockchainApi.addConfiguration(eContext, lastHeight + preemption, override = true, gtvWithSigners, allowUnknownSigners = true)
                echo("Configuration added at height ${lastHeight + preemption}")
            }
        }
    }
}
