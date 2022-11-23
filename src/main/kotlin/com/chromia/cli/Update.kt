package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator.generateConfig
import com.chromia.cli.compile.config.DeploymentConfigGenerator.generatePostchainClientConfig
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.common.proposal.proposeConfigurationOperation
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.utils.RellCliErr

class UpdateCommand : CliktCommand(help = "Update configuration of running blockchain") {
    private val outputDir by outputDirOption()
    private val blockchainRid by brid()
    private val sourceFile by sourceDirOption()
    private val config by configFile()
    private val nodeConfig by nodeConfigFile()
    private val target by deployTarget()

    override fun run() {

        val nodeAppConf = nodeConfig?.let { NodeConfig.getNodeConfig(it) } ?: NodeConfig.getDefaultNodeConfig(config)
        val cSourceDir = C_SourceDir.diskDir(sourceFile)

        BlockchainConfigurationGenerator(config, cSourceDir, nodeAppConf).generateMaps().toList().forEach {(brid, gtv) ->

            if (target != null) {
                val selectiveDeployment = config.deployment[target]

                if(selectiveDeployment?.apiUrl == null) {
                    throw RellCliErr("No url for model found")
                }
                val clientConfig = generatePostchainClientConfig(brid, EndpointPool.singleUrl(selectiveDeployment.apiUrl))
                generateConfig(gtv, selectiveDeployment, "${target}_${brid}", brid, outputDir).apply {
                    updateBlockchain(
                            clientConfig,
                            blockchainRid ?: this.specifiedBlockchainRid
                            ?: throw CliktError("No blockchain-rid specified"),
                            GtvEncoder.encodeGtv(this.configuration),
                            PostchainClientProviderImpl()
                    )
                }

            } else {
                config.deployment.toList().forEach{ (name, model) ->
                    val clientConfig = generatePostchainClientConfig(brid, EndpointPool.singleUrl(model.apiUrl))
                    generateConfig(gtv, model, "${name}_${brid}", brid, outputDir).apply {
                        updateBlockchain(
                                clientConfig,
                                blockchainRid ?: this.specifiedBlockchainRid
                                ?: throw CliktError("No blockchain-rid specified"),
                                GtvEncoder.encodeGtv(this.configuration),
                                PostchainClientProviderImpl()
                        )
                    }
                    }
            }
        }
    }

    private fun updateBlockchain(
            clientConfig: PostchainClientConfig,
            blockchainRid: BlockchainRid,
            configData: ByteArray,
            clientProvider: PostchainClientProvider
    ) {
        val client = clientProvider.createClient(clientConfig)
        val result = client
                .transactionBuilder()
                .proposeConfigurationOperation(
                        clientConfig.signers.first().pubKey.data,
                        blockchainRid,
                        configData
                )
                .sign()
                .postAwaitConfirmation()
        if (result.status != TransactionStatus.CONFIRMED) {
            throw UserMistake("Update failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        }
    }
}