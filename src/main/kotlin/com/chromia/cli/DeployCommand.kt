package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator
import com.chromia.cli.compile.config.NamedBlockchainRid
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
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.chain0.common.proposal.proposeBlockchainOperation
import net.postchain.chain0.common.proposal.proposeConfigurationOperation
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.compiler.base.utils.C_SourceDir
import org.apache.commons.configuration2.BaseConfiguration
import java.time.Instant.now
import java.util.*

class DeployCommand : CliktCommand(help = "Deploy blockchain into container") {
    private val showBrid by showBridOption()
    private val settings by settingsOption()
    private val target by deployTargetOption().required()
    private val blockchain by option(help = "Name of blockchain to deploy")
    private val secret by secretOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        val cSourceDir = C_SourceDir.diskDir(settings.compile.source)

        val generator = BlockchainConfigurationGenerator(settings, cSourceDir)
        val chainsToDeploy = blockchain?.let {
            require(settings.blockchains[it] != null) { "Specified blockchain $it does not exist" }
            listOf(generator.generateConfiguration(it, settings.blockchains[it]!!))
        } ?: generator.generate().toList()
        chainsToDeploy.forEach { (generatedBlockchainRid, gtv) ->

            val deployModel = settings.deployments[target]
            require(deployModel != null) { "deployment target with name $target not found" }


            val clientConfig = BaseConfiguration().apply {
                setProperty("api.url", deployModel.apiUrl)
                setProperty("brid", deployModel.blockchainRid.toHex())
                secret?.let { s ->
                    Properties().apply { load(s.inputStream()) }.let { p ->
                        p["pubkey"]?.let { setProperty("pubkey", it) }
                        p["privkey"]?.let { setProperty("privkey", it) }
                    }
                }
            }.let { PostchainClientConfig.fromConfiguration(it) }
            DeploymentConfigGenerator.generateConfig(gtv, deployModel, "${target}_${generatedBlockchainRid.name}_${now()}", generatedBlockchainRid.blockchainRid, settings.compile.target.toPath(), generatedBlockchainRid.name)
                    .apply {
                        if (specifiedBlockchainRid == null) {
                            verifyNewDeployment(generatedBlockchainRid)
                        }
                        deployBlockchain(
                                clientConfig,
                                blockchainName,
                                deployModel.container ?: throw CliktError("No container specified"),
                                GtvEncoder.encodeGtv(this.configuration),
                                PostchainClientProviderImpl(),
                                specifiedBlockchainRid
                        )
                        if (showBrid) echo(specifiedBlockchainRid ?: generatedBlockchainRid.blockchainRid)
                    }
        }
    }

    private fun verifyNewDeployment(namedBlockchainRid: NamedBlockchainRid): Unit? {
        println("BlockchainRid for chain ${namedBlockchainRid.name} on deployment $target not set. Would you like to create a new deployment? Y / N")
        val resp = Scanner(System.`in`).nextLine().equals("Y", true)
        if (resp) {
            println("Add the following to your project settings file")
            println("""
                                    deployments:
                                      $target:
                                        chains:
                                          ${namedBlockchainRid.name}: x"${namedBlockchainRid.blockchainRid.toHex()}"
                                """.trimIndent())
            return null
        } else {
            throw CliktError("Failed to deploy")
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
                .addNop()
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
            throw CliktError("Deployment failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        } else {
            echo("Deployment of blockchain $blockchainName was successful")
        }
    }
}
