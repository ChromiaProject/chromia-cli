package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.tools.config.ChromiaModelConfigOption
import com.chromia.cli.tools.config.client
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.secretOption
import com.chromia.directory1.proposal_blockchain.findBlockchainRid
import com.chromia.directory1.version.apiVersion
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import java.time.Instant
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.crypto.sha256Digest

class DeploymentCommand : NoOpCliktCommand(help = "Create and maintain deployments") {
    override fun aliases() = createAliases()
}

fun deployCommands() = DeploymentCommand().subcommands(
        DeployCreateCommand(),
        DeployInfoCommand(),
        DeployInspectCommand(),
        DeployUpdateCommand(),
        DeployResumeCommand(),
        DeployPauseCommand()
)

abstract class AbstractDeploymentCommand(name: String, help: String, protected val clientProvider: PostchainClientProvider) : CliktCommand(name = name, help = help) {

    protected val settings by ChromiaModelConfigOption()
    private val secret by secretOption()
    protected val target by deployTargetOption().required()
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.model.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }

    protected val deployModel by lazy {
        val deployModel = settings.model.deployments[target]
                ?: throw PrintMessage("deployment target with name $target not found")
        if (deployModel.container == null) throw PrintMessage("No container specified on network $target")
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
        val compiledChains = ChromiaCompileApi.compile(CliktCliEnv(this@AbstractDeploymentCommand), settings.model, settings.modelFile.parentFile, chainsToDeploy)
        beforeDeployment(chainsToDeploy)

        val client = createClient()
        val apiVersion = client.apiVersion()
        var failure = false
        val txs = buildList {
            for (chain in compiledChains) {
                val result = client
                        .transactionBuilder()
                        .addNop()
                        .apply { addDeploymentOperation(client, client.config, chain) }
                        .sign()
                        .post()
                if (result.status == TransactionStatus.REJECTED) {
                    echo("Deployment of blockchain ${chain.name} failed: ${result.rejectReason ?: ""}", err = true)
                    failure = true
                } else {
                    add(chain to result.txRid)
                }
            }
        }

        val deployChains = buildList {
            for ((chain, tx) in txs) {
                val result = client.awaitConfirmation(tx, client.config.statusPollCount, client.config.statusPollInterval)
                when (result.status) {
                    TransactionStatus.CONFIRMED -> {
                        chain.save(settings.targetDir, "${target}_${chain.name}_${Instant.now().toEpochMilli()}")
                        val maybeBcRid = if (apiVersion >= 8) {
                            client.findBlockchainRid(tx.rid.hexStringToByteArray())?.let { BlockchainRid(it) }
                        } else {
                            GtvToBlockchainRidFactory.calculateBlockchainRid(chain.config, ::sha256Digest)
                        }
                        if (maybeBcRid != null) {
                            echo("Deployment of blockchain ${chain.name} was successful")
                            add(chain.name to maybeBcRid)
                        } else {
                            echo("Deployment of blockchain ${chain.name} was proposed, tx-rid: ${tx.rid}")
                        }
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

        afterDeployment(deployChains)

        if (failure) {
            throw ProgramResult(1)
        }
    }

    abstract fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult)

    abstract fun beforeDeployment(deployedChains: Collection<String>)

    abstract fun afterDeployment(deployedChains: List<Pair<String, BlockchainRid>>)

    private fun chainsToDeploy(): Collection<String> {
        return blockchain ?: settings.model.blockchains.keys
    }
}
