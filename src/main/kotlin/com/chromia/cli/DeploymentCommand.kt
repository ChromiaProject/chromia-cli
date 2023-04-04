package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.BlockchainConfigurationWriter
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.createAliases
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.secretOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.tx.TransactionStatus
import net.postchain.rell.compiler.base.utils.C_SourceDir
import org.apache.commons.configuration2.BaseConfiguration
import java.io.File
import java.time.Instant
import java.util.Properties

class DeploymentCommand : NoOpCliktCommand(help = "Create and maintain deployments") {
    override fun aliases() = createAliases()
}

fun deployCommands() = DeploymentCommand().subcommands(
        DeployCreateCommand(),
        DeployInfoCommand(),
        DeployInspectCommand(),
        DeployUpdateCommand(),
        DeployResumeCommand(),
        DeployPauseCommand(),
        DeployRemoveCommand()
)

abstract class AbstractDeploymentCommand(name: String, help: String, protected val clientProvider: PostchainClientProvider) : CliktCommand(name = name, help = help) {

    protected val settings by settingsOption()
    private val secret by secretOption().defaultLazy { File(".secret") }
    protected val target by deployTargetOption().required()
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }

    protected val deployModel by lazy {
        val deployModel = settings.deployments[target]
                ?: throw PrintMessage("deployment target with name $target not found")
        if (deployModel.container == null) throw PrintMessage("No container specified on network $target")
        deployModel
    }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    private fun createClient(): PostchainClient {
        return BaseConfiguration().apply {
            setProperty("api.url", deployModel.urls.joinToString(","))
            setProperty("brid", deployModel.blockchainRid.toHex())
            secret.let { s ->
                Properties().apply { load(s.inputStream()) }.let { p ->
                    p["pubkey"]?.let { setProperty("pubkey", it) }
                    p["privkey"]?.let { setProperty("privkey", it) }
                }
            }
        }
                .let { PostchainClientConfig.fromConfiguration(it) }
                .let { clientProvider.createClient(it) }
    }

    final override fun run() {
        val cSourceDir = C_SourceDir.diskDir(settings.source)

        val generator = BlockchainConfigurationGenerator(CliktCliEnv(this), settings.compile, settings.blockchains, cSourceDir)
        val chainsToDeploy = chainsToDeploy(generator)
        beforeDeployment(chainsToDeploy)
        val client = createClient()
        val result = client
                .transactionBuilder()
                .addNop()
                .apply { chainsToDeploy.forEach { addDeploymentOperation(client, client.config, it) } }
                .sign()
                .postAwaitConfirmation()
        if (result.status != TransactionStatus.CONFIRMED) {
            throw CliktError("Deployment failed: ${result.rejectReason ?: "still waiting for confirmation"}")
        } else {
            echo("Deployment of blockchain ${chainsToDeploy.joinToString(", ") { it.name }} was successful")
        }

        chainsToDeploy.forEach { configHolder ->
            BlockchainConfigurationWriter.storeConfig(configHolder.config, "${target}_${configHolder.name}_${Instant.now()}", settings.target.toPath())
        }

        afterDeployment(chainsToDeploy)
    }

    abstract fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: BlockchainConfigHolder)

    abstract fun beforeDeployment(deployedChains: Collection<BlockchainConfigHolder>)

    abstract fun afterDeployment(deployedChains: Collection<BlockchainConfigHolder>)

    private fun chainsToDeploy(generator: BlockchainConfigurationGenerator): Collection<BlockchainConfigHolder> {
        return blockchain?.let { chains ->
            chains.map { generator.generateConfiguration(it, settings.blockchains[it]!!) }
        } ?: generator.generate().toList()
    }
}
