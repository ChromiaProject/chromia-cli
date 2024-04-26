package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.build.tools.compile.BlockchainConfigurationWriter
import com.chromia.build.tools.config.client
import com.chromia.cli.tools.config.BlockchainConfigurationCompressor
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.filterGtxModules
import com.chromia.cli.util.secretOption
import com.chromia.cli.versionfinder.CanNotFindBlockchainException
import com.chromia.cli.versionfinder.NoNodeRunningContainerException
import com.chromia.cli.versionfinder.PostchainRellVersionFinder
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.chromia.directory1.common.queries.getClusterApiUrls
import com.chromia.directory1.common.queries.getContainerData
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import java.time.Instant
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.core.TxRid
import net.postchain.client.request.Endpoint
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.tx.TransactionStatus
import org.http4k.core.HttpHandler

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

    private fun createClientConfig(): PostchainClientConfig {
        return settings.model.client(settings.config, secret = secret, network = target, blockchain = null)
    }

    final override fun run() {
        val chainsToDeploy = chainsToDeploy()
        val cliEnv = CliktCliEnv(this@AbstractDeploymentCommand)
        val compiledChains = ChromiaCompileApi.build(cliEnv,
                settings.model.filterBlockchains(chainsToDeploy), settings.projectFolder.toPath())
                .onEach { it.filterGtxModules(cliEnv).validate() }
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
                        chain.save(settings.targetDir.toPath(), "${target}_${chain.name}_${Instant.now().toEpochMilli()}")
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

    abstract fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: BlockchainConfiguration)

    abstract fun beforeDeployment(compiledChains: Collection<BlockchainConfiguration>, client: PostchainClient)

    abstract fun afterDeployment(client: PostchainClient, deployTxs: List<Pair<BlockchainConfiguration, TxRid>>)

    private fun chainsToDeploy(): Collection<String> {
        return blockchain ?: settings.model.blockchains.keys
    }

    protected fun configToDeploy(chain: BlockchainConfiguration): BlockchainConfiguration {
        return if (noCompression) {
            chain
        } else {
            val configWithCompressionInfo = BlockchainConfigurationCompressor.compress(client, chain.config, client.apiVersion)
            BlockchainConfigurationWriter.storeConfig(configWithCompressionInfo, "${chain.name}_compressed", settings.model.compile.targetFile(settings.projectFolder).toPath())
            return BlockchainConfiguration(chain.name, configWithCompressionInfo)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    protected fun validateRellVersion(client: PostchainClient, httpHandlerFactory: (PostchainClientConfig) -> HttpHandler) {
        val rellVersionController = PostchainRellVersionFinder(client.config, clientProvider)

        val clusterName = client.getContainerData(deployModel.container!!).cluster
        val clusterNodeUrls = client.getClusterApiUrls(clusterName)

        val targetVersions = clusterNodeUrls.mapNotNull {
            try {
                rellVersionController.getTargetVersion(Endpoint(it), deployModel.blockchainRid)
            } catch (e: CanNotFindBlockchainException) {
                throw e
            } catch (e: RuntimeException) {
                echo(e.message)
                null
            }
        }

        if (targetVersions.isEmpty()) {
            throw NoNodeRunningContainerException(deployModel.container!!)
        }

        if (targetVersions.any { it < settings.model.compile.langVersion }) {
            throw RellDeployVersionException(settings.model.compile.rellVersion, targetVersions.min())
        }
    }
}
