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
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.common.proposal.proposeBlockchainOperation
import net.postchain.d1.common.proposal.proposeConfigurationOperation
import net.postchain.gtv.GtvEncoder
import net.postchain.rell.compiler.base.utils.C_SourceDir
import org.apache.commons.configuration2.BaseConfiguration
import java.time.Instant.now
import java.util.*

class DeployCommand : CliktCommand(help = "Deploy blockchain into container") {
    private val outputDir by outputDirOption()
    private val showBrid by showBridOption()
    private val sourceDir by sourceDirOption()
    private val settings by settingsOption()
    private val target by deployTargetOption().required()
    private val blockchain by option(help = "Name of blockchain to deploy")
    private val secret by option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, canBeFile = true)

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
            DeploymentConfigGenerator.generateConfig(gtv, deployModel, "${target}_${namedBlockchainRid.name}_${now()}", namedBlockchainRid.blockchainRid, outputDir, namedBlockchainRid.name)
                    .apply {
                        val brid = if (specifiedBlockchainRid == null) {
                            println("BlockchainRid for chain ${namedBlockchainRid.name} on deployment $target not set. Would you like to create a new deployment? true/false")
                            val resp = Scanner(System.`in`).nextBoolean() // TODO: Use a less crappy scanner
                            if (resp) {
                                println("Add ${namedBlockchainRid.name}: 0x${generatedBlockchainRid.toHex()} to deployments:chains:")
                                null
                            } else {
                                throw CliktError("Failed to deploy")
                            }
                        } else specifiedBlockchainRid
                        deployBlockchain(
                                clientConfig,
                                blockchainName,
                                deployModel.licence ?: throw CliktError("No container specified"),
                                GtvEncoder.encodeGtv(this.configuration),
                                PostchainClientProviderImpl(),
                                brid
                        )
                        if (showBrid) echo(brid)
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
            throw CliktError("Deployment failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        } else {
            echo("Deployment of blockchain $blockchainName was successful")
        }
    }
}
