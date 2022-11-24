package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.common.proposal.proposeBlockchainOperation
import net.postchain.d1.common.proposal.proposeConfigurationOperation
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.compiler.base.utils.C_SourceDir
import java.time.Instant.now

class DeployCommand : CliktCommand(help = "Deploy blockchain into container") {
    private val outputDir by outputDirOption()
    private val showBrid by showBridOption()
    private val sourceDir by sourceDirOption()
    private val settings by settingsOption()
    private val target by deployTargetOption().required()
    private val blockchain by option(help = "Name of blockchain to deploy")

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        val cSourceDir = C_SourceDir.diskDir(sourceDir)

        val generator = BlockchainConfigurationGenerator(settings, cSourceDir)
        val chainsToDeploy = blockchain?.let {
            require(settings.blockchains[it] != null) { "Specified blockchain $it does not exist" }
            listOf(generator.generateConfiguration(it, settings.blockchains[it]!!))
        } ?: generator.generate().toList()
        chainsToDeploy.forEach { (namedBlockchainRid, gtv) ->

            require(settings.deployment[target] != null) { "deployment target with name $target not found" }
            val deployModel = settings.deployment[target]!!

            val clientConfig = DeploymentConfigGenerator.generatePostchainClientConfig(namedBlockchainRid.blockchainRid, EndpointPool.singleUrl(deployModel.apiUrl))
            DeploymentConfigGenerator.generateConfig(gtv, deployModel, "${target}_${namedBlockchainRid.name}_${now()}", namedBlockchainRid.blockchainRid, outputDir).apply {
                deployBlockchain(
                        clientConfig,
                        this.blockchainName,
                        this.containerName ?: this.containerName ?: throw CliktError("No container specified"),
                        GtvEncoder.encodeGtv(this.configuration),
                        PostchainClientProviderImpl(),
                        specifiedBlockchainRid
                )
                if (showBrid) echo(specifiedBlockchainRid ?: generatedBlockchainRid)
            }
        }
    }

    private fun deployBlockchain(
            clientConfig: PostchainClientConfig,
            blockchainName: String,
            containerName: String,
            configData: ByteArray,
            clientProvider: PostchainClientProvider,
            optionalBrid: BlockchainRid?
    ) {
        val client = clientProvider.createClient(clientConfig)
        val result = client
                .transactionBuilder()
                .apply {
                    if (optionalBrid == null) {
                        proposeBlockchainOperation(
                                clientConfig.signers.first().pubKey.data,
                                configData,
                                blockchainName,
                                containerName
                        )
                    } else {
                        proposeConfigurationOperation(
                                clientConfig.signers.first().pubKey.data,
                                optionalBrid,
                                configData
                        )
                    }
                }
                .sign()
                .postAwaitConfirmation()
        if (result.status != TransactionStatus.CONFIRMED) {
            throw UserMistake("Deployment failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        } else {
            echo("Deployment of blockchain $blockchainName was successful")
        }
    }
}
