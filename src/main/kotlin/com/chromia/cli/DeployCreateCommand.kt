package com.chromia.cli

import com.chromia.cli.compatibility.BlockchainOperations
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
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvEncoder
import net.postchain.gtx.GTXBlockchainConfigurationFactory
import net.postchain.rell.compiler.base.utils.C_SourceDir
import org.apache.commons.configuration2.BaseConfiguration
import java.time.Instant.now
import java.util.*

class DeployCreateCommand(val clientProvider: PostchainClientProviderImpl = PostchainClientProviderImpl()) : CliktCommand(name = "create", help = "Deploy blockchain into container") {
    private val showBrid by showBridOption()
    private val settings by settingsOption()
    private val target by deployTargetOption().required()
    private val blockchain by option(help = "Name of blockchain to deploy")
    private val height by option(help = "Deploy configuration at a specific height").long()
    private val secret by secretOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        val cSourceDir = C_SourceDir.diskDir(settings.source)

        val generator = BlockchainConfigurationGenerator(CliktCliEnv(this), settings.compile, settings.blockchains, cSourceDir)
        val chainsToDeploy = blockchain?.let {
            require(settings.blockchains[it] != null) { "Specified blockchain $it does not exist" }
            listOf(generator.generateConfiguration(it, settings.blockchains[it]!!))
        } ?: generator.generate().toList()
        chainsToDeploy.forEach { (generatedBlockchainRid, gtv) ->

            val deployModel = settings.deployments[target]
            require(deployModel != null) { "deployment target with name $target not found" }


            val clientConfig = BaseConfiguration().apply {
                setProperty("api.url", deployModel.urls.joinToString(","))
                setProperty("brid", deployModel.blockchainRid.toHex())
                secret?.let { s ->
                    Properties().apply { load(s.inputStream()) }.let { p ->
                        p["pubkey"]?.let { setProperty("pubkey", it) }
                        p["privkey"]?.let { setProperty("privkey", it) }
                    }
                }
            }.let { PostchainClientConfig.fromConfiguration(it) }
            DeploymentConfigGenerator.generateConfig(gtv, deployModel, "${target}_${generatedBlockchainRid.name}_${now()}", generatedBlockchainRid.blockchainRid, settings.target.toPath(), generatedBlockchainRid.name)
                    .apply {
                        val extraMessage = if (specifiedBlockchainRid == null) {
                            verifyNewDeployment(generatedBlockchainRid)
                        } else null
                        deployBlockchain(
                                clientConfig,
                                blockchainName,
                                deployModel.container ?: throw CliktError("No container specified"),
                                GtvEncoder.encodeGtv(this.configuration),
                                clientProvider,
                                specifiedBlockchainRid
                        )
                        if (showBrid) echo(specifiedBlockchainRid ?: generatedBlockchainRid.blockchainRid)
                        extraMessage?.let { echo(it) }
                    }
        }
    }

    private fun verifyNewDeployment(namedBlockchainRid: NamedBlockchainRid): String {
        confirm(
                "Blockchain RID for chain ${namedBlockchainRid.name} on deployment $target not set. Would you like to create a new deployment?",
                default = false, abort = true)
        return """
            Add the following to your project settings file:
            deployments:
              $target:
                chains:
                  ${namedBlockchainRid.name}: x"${namedBlockchainRid.blockchainRid.toHex()}"
            """.trimIndent()
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
                    val blockchainOperations = BlockchainOperations(client.directory1Version, this)
                    if (optionalBrid == null) {
                        blockchainOperations.newBlockchainOperation(
                                clientConfig.signers.first().pubKey.data,
                                configData,
                                blockchainName,
                                containerName
                        )
                    } else {
                        blockchainOperations.proposeConfiguration(
                                clientConfig.signers.first().pubKey.data,
                                optionalBrid,
                                configData,
                                height,
                                true
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
