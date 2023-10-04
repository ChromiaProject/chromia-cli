package com.chromia.cli

import com.chromia.build.tools.compile.withSigner
import com.chromia.cli.compile.ConfigExtractor
import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.model.parseModel
import com.chromia.cli.tools.config.ChromiaConfigLoader
import com.chromia.cli.tools.config.chromiaModelFileOption
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.util.logSqlOption
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.wipeDatabaseOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import mu.withLoggingContext
import net.postchain.PostchainNode
import net.postchain.StorageInitializer
import net.postchain.api.internal.BlockchainApi
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.base.runStorageCommand
import net.postchain.base.withReadWriteConnection
import net.postchain.common.BlockchainRid
import net.postchain.core.EContext
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.crypto.sha256Digest
import net.postchain.logging.BLOCKCHAIN_RID_TAG
import net.postchain.logging.CHAIN_IID_TAG
import net.postchain.logging.NODE_PUBKEY_TAG
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.server.cli.waitDb

class StartCommand : AbstractNodeCommand(help = """
    Starts a test node
    
    If a blockchain has already been started on the configured database schema, the configuration will be added to the next height such that the node will be started with the new config. 
    Use --wipe to wipe the database schema upon startup and thus enforce starting the chain from height=0.
""".trimIndent()) {
    private val settings by chromiaModelFileOption()
    private val sqlLog by logSqlOption()
    private val wipe by wipeDatabaseOption()
    val cryptoSystem = Secp256K1CryptoSystem()
    private val service by option(help = "Wait for resources to be availabe before starting (Useful for CI)", envvar = "CHROMIA_SERVICE").flag()
    private val overrides by option("-p", help = "Override any property value (usage: -p key=value)", metavar = "KEY=VALUE").associate()
    private val nodeConfigFile by nodePropertiesOption()

    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        echo("Starting test node")
        val modelFile =  if (service) waitForFile(100, 500) { findModelFile() } else findModelFile()
        val model = modelFile?.let { parseModel(it) } ?: throw PrintMessage("Model not found", statusCode = 1)
        val nodeConfig by lazy { nodeConfigFile ?: NodeConfig.getDefaultNodeConfig(model, overrides) }
        if (service) waitDb(100, 500, nodeConfig)

        val chainsToStart = mutableListOf<Long>()
        val environment = RellPostchainModuleEnvironment(
                sqlLog = sqlLog
        )
        RellPostchainModuleEnvironment.set(environment) {
            val node = PostchainNode(nodeConfig, wipeDb = wipe)

            ConfigExtractor(model, cliEnv()).extractConfigs(modelFile.parentFile, blockchainConfigs, name).toList().forEachIndexed { index, (_, gtv) ->
                val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
                val brid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners, ::sha256Digest)
                val iid = index.toLong()
                echo("Starting chain $brid on id $iid")
                chainsToStart.add(iid)
                withLoggingContext(
                        NODE_PUBKEY_TAG to nodeConfig.pubKey,
                        CHAIN_IID_TAG to iid.toString(),
                        BLOCKCHAIN_RID_TAG to brid.toHex()
                ) {
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
                            val blockchainRid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners, cryptoSystem)

                            if (previousBlockchainRids.contains(blockchainRid)) throw PrintMessage("Blockchain configuration already exists in database, cannot start on already used config")
                            if (blockchainRid != lastBlockchainRid) BlockchainApi.addConfiguration(eContext, lastHeight + 1, override = true, gtvWithSigners)
                        }
                    }
                }
            }
            runStorageCommand(nodeConfig) {
                StorageInitializer.setupInitialPeers(nodeConfig, it)
            }
            chainsToStart.forEach {
                node.startBlockchain(it)
            }
        }
    }

    private fun findModelFile() = ChromiaConfigLoader(cliEnv()).findModelFile(settings)

    private fun waitForFile(retryTimes: Int, retryInterval: Long, fileFinder: () -> File?): File {
        val file = fileFinder()
        if (file == null) {
           if (retryTimes <= 0) throw PrintMessage("File does not exist")
            Thread.sleep(retryInterval)
            waitForFile(retryTimes - 1, retryInterval, fileFinder)
        }
        return file!!
    }
}
