package com.chromia.cli

import com.chromia.cli.exception.RellDeployVersionException
import com.chromia.cli.interfaces.RellVersionControllerInterface
import com.chromia.cli.util.RellVersionController
import com.chromia.cli.util.withSigner
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
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.gtv.GtvDecoder

class UpdateCommand(
        private val rellVersionController: () -> RellVersionControllerInterface = { RellVersionController() }
) : AbstractNodeCommand(help = """
    Updates a running test node
    
    Will add a configuration to a block height 5 higher that current height for the running blockchain. 
    Make sure this command is executed with exactly the same config.yml and arguments as was used when starting the node using `chr node start` to make sure configurations are added to the correct chain ids.
""".trimIndent()) {

    private val preemption by option("-n", "--preemption", help = "Update the configuration at a height this many blocks into the future")
            .int().default(2)
            .validate { require(it > 1) { "Must be more than one block in the future" } }
    val cryptoSystem = Secp256K1CryptoSystem()

    override fun run() {
        val storage = StorageBuilder.buildStorage(nodeConfig, false)

        val targetVersion = rellVersionController().getTargetVersion(settings)
        if (targetVersion != settings.compile.rellVersion) {
            throw RellDeployVersionException(settings.compile.rellVersion, targetVersion)

        }


        extractConfigs().toList().forEachIndexed { index, (_, _, gtv) ->
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
