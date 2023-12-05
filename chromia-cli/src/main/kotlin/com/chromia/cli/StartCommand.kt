package com.chromia.cli

import com.chromia.build.tools.compile.withSigner
import com.chromia.cli.util.logSqlOption
import com.chromia.cli.util.wipeDatabaseOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.option
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import kotlin.system.exitProcess
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

class StartCommand : AbstractNodeCommand(help = """
    Starts a test node
    
    If a blockchain has already been started on the configured database schema, the configuration will be added to the next height such that the node will be started with the new config. 
    Use --wipe to wipe the database schema upon startup and thus enforce starting the chain from height=0.
""".trimIndent()) {
    private val sqlLog by logSqlOption()
    private val wipe by wipeDatabaseOption()
    val cryptoSystem = Secp256K1CryptoSystem()
    private val script by option(help = "Script to run")

    override fun run() {
        startPostchainNode()
    }

    private fun startPostchainNode() {
        val chainsToStart = mutableListOf<Long>()
        val environment = RellPostchainModuleEnvironment(
                sqlLog = sqlLog
        )
        RellPostchainModuleEnvironment.set(environment) {
            echo("Starting node with pubkey: ${nodeConfig.pubKey}")
            val node = PostchainNode(nodeConfig, wipeDb = wipe)

            extractConfigs().toList().forEachIndexed { index, (name, gtv) ->
                val gtvWithSigners = withSigner(gtv, nodeConfig.pubKeyByteArray)
                val brid = GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners, ::sha256Digest)
                val iid = index.toLong()
                echo("Starting blockchain $name with brid $brid on id $iid")
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
            script?.let { s ->
                val process = ProcessBuilder(s.split(" "))
                        .directory(File("."))
                        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                        .start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                while (process.isAlive) {
                    reader.readLine()?.let { echo(it) }
                }
                exitProcess(process.waitFor())
            }
        }
    }
}
