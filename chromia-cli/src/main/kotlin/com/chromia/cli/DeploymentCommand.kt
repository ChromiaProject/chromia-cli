package com.chromia.cli

import com.chromia.build.tools.compile.BlockchainConfigurationWriter
import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.tools.config.BlockchainConfigurationCompressor
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.client
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.core.TxRid
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.tx.TransactionStatus
import java.time.Instant

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

    protected val settings by chromiaModelConfigOption()
    private val secret by secretOption()
    protected val target by deployTargetOption().required()
            .validate { require(settings.model.deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.model.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }
    private val noCompression by option("--no-compression", help = "If compression on rell sources should not be done").flag()

    protected val client by lazy {
        val client = createClient()
        if (client.config.signers.isEmpty()) {
            throw PrintMessage("To be able to deploy, you must specify signer keys. Either using --secret file or in the config.", statusCode = 1)
        }
        client
    }

    protected val deployModel by lazy {
        val deployModel = settings.model.deployments[target]
        if (deployModel!!.container == null) throw PrintMessage("No container specified on network $target")
        deployModel
    }

    private fun createClient(): PostchainClient {
        return createClientConfig()
                .let { clientProvider.createClient(it) }
    }

    protected fun createClientConfig(): PostchainClientConfig {
        return settings.model.client(settings.config, secret = secret, network = target, blockchain = null)
    }

    final override fun run() {
        val chainsToDeploy = chainsToDeploy()
        val compiledChains = ChromiaCompileApi.compile(CliktCliEnv(this@AbstractDeploymentCommand), settings.model, settings.projectFolder, chainsToDeploy)
        beforeDeployment(compiledChains, client)

        var failure = false
        val txs = buildList {
            for (chain in compiledChains) {
                val result = client
                        .transactionBuilder()
                        .addNop()
                        .apply { addDeploymentOperation(client, client.config, configToDeploy(chain)) }
                        .post()
                if (result.status == TransactionStatus.REJECTED) {
                    echo("Deployment of blockchain ${chain.name} failed: ${result.rejectReason ?: ""}", err = true)
                    failure = true
                } else {
                    add(chain to result.txRid)
                }
            }
        }

        val deployTxs = buildList {
            for ((chain, tx) in txs) {
                val result = client.awaitConfirmation(tx, client.config.statusPollCount, client.config.statusPollInterval)
                when (result.status) {
                    TransactionStatus.CONFIRMED -> {
                        chain.save(settings.targetDir, "${target}_${chain.name}_${Instant.now().toEpochMilli()}")
                        add(chain to tx)
                    }

                    TransactionStatus.REJECTED -> {
                        echo("Deployment of blockchain ${chain.name} failed: ${result.rejectReason ?: ""}", err = true)
                        failure = true
                    }

                    TransactionStatus.WAITING -> echo("Deployment of blockchain ${chain.name} still pending, tx-rid: ${tx.rid}")
                    else -> throw CliktError("Cannot find status for this transaction")
                }
            }
        }

        afterDeployment(client, deployTxs)

        if (failure) {
            throw ProgramResult(1)
        }
    }

    abstract fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult)

    abstract fun beforeDeployment(compiledChains: Collection<ChromiaCompileResult>, client: PostchainClient)

    abstract fun afterDeployment(client: PostchainClient, deployTxs: List<Pair<ChromiaCompileResult, TxRid>>)

    private fun chainsToDeploy(): Collection<String> {
        return blockchain ?: settings.model.blockchains.keys
    }

    protected fun configToDeploy(chain: ChromiaCompileResult): ChromiaCompileResult {
        return if (noCompression) {
            chain
        } else {
            val configWithCompressionInfo = BlockchainConfigurationCompressor.compress(client, chain.config, client.apiVersion)
            BlockchainConfigurationWriter.storeConfig(configWithCompressionInfo, "${chain.name}_compressed", settings.model.compile.targetFile(settings.projectFolder).toPath())
            return ChromiaCompileResult(chain.name, configWithCompressionInfo)
        }
    }
}
