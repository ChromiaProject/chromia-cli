package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.common.proposal.proposeBlockchainOperation
import net.postchain.deployment.ChromiaDeploymentTool
import net.postchain.deployment.DeploymentTool
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.utils.RellCliErr
import java.nio.file.Path
import kotlin.io.path.absolutePathString


private fun CliktCommand.deployXmlOption() =
    argument(name = "deploy.xml", ).path(mustExist = true, canBeDir = false, canBeFile = true, mustBeReadable = true).default(Path.of("rell/config/deploy.xml"))

private fun ParameterHolder.clientConfigOption() = option("--config", help = "Client configuration *.properties")
    .path(mustExist = true, canBeDir = false, canBeFile = true, mustBeReadable = true)

class DeployCommand : CliktCommand(help = "Deploy blockchain into container") {
    private val clientConfig by clientConfigOption()

    private val sourceDir by sourceDirOption()
    private val outputDir by outputDirOption()
    private val deployXmlFile by deployXmlOption()

    private val containerName by containerOption()
    private val showBrid by showBridOption()
    private val sourceFile by sourceDirOption()
    private val config by configFile()
    private val nodeConfig by nodeConfigFile()
    private val target by deployTarget()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        val nodeAppConf = nodeConfig?.let { NodeConfig.getNodeConfig(it) } ?: NodeConfig.getDefaultNodeConfig(config)
        val cSourceDir = C_SourceDir.diskDir(sourceFile)

        BlockchainConfigurationGenerator(config, cSourceDir, nodeAppConf).generateMaps().toList().forEach { (brid, gtv) ->

            if (target != null) {
                val selectiveDeployment = config.deployment[target]

                if(selectiveDeployment?.apiUrl == null) {
                    throw RellCliErr("No url for model found")
                }
                val clientConfig = DeploymentConfigGenerator.generatePostchainClientConfig(brid, EndpointPool.singleUrl(selectiveDeployment.apiUrl))
                DeploymentConfigGenerator.generateConfig(gtv, selectiveDeployment, "${target}_${brid}", brid, outputDir).apply {
                    deployBlockchain(
                            clientConfig,
                            this.blockchainName,
                            this.containerName ?: this.containerName ?: throw CliktError("No container specified"),
                            GtvEncoder.encodeGtv(this.configuration),
                            PostchainClientProviderImpl()
                    )
                    if (showBrid) echo(this.generatedBlockchainRid)
                }

            } else {
                config.deployment.toList().forEach{ (name, model) ->
                    val clientConfig = DeploymentConfigGenerator.generatePostchainClientConfig(brid, EndpointPool.singleUrl(model.apiUrl))
                    DeploymentConfigGenerator.generateConfig(gtv, model, "${name}_${brid}", brid, outputDir).apply {
                        deployBlockchain(
                                clientConfig,
                                this.blockchainName,
                                this.containerName ?: this.containerName ?: throw CliktError("No container specified"),
                                GtvEncoder.encodeGtv(this.configuration),
                                PostchainClientProviderImpl()
                        )
                        if (showBrid) echo(this.generatedBlockchainRid)
                    }
                }
            }

        }
    }

    private fun deployBlockchain(
            clientConfig: PostchainClientConfig,
            blockchainName: String,
            containerName: String,
            configData: ByteArray,
            clientProvider: PostchainClientProvider
    ) {
        val client = clientProvider.createClient(clientConfig)
        val result = client
                .transactionBuilder()
                .proposeBlockchainOperation(
                        clientConfig.signers.first().pubKey.data,
                        configData,
                        blockchainName,
                        containerName
                )
                .sign()
                .postAwaitConfirmation()
        if (result.status != TransactionStatus.CONFIRMED) {
            throw UserMistake("Deployment failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        }
    }

}
