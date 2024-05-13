package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.api.result.isSuccess
import com.chromia.api.result.save
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.env.CliktCliEnv
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
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import java.time.Clock
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.Endpoint

abstract class AbstractDeploymentCommand(name: String, help: String, protected val clientProvider: PostchainClientProvider) : CliktCommand(name = name, help = help) {

    protected val settings by chromiaModelConfigOption()
    private val secret by secretOption()
    protected val target by deployTargetOption().required()
            .validate { require(settings.model.deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.model.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }
    protected val noCompression by option("--no-compression", help = "If compression on rell sources should not be done").flag()

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
        val model = settings.model.deployments[target]
        require(model != null) { "Network $target is not a configured deployment" }
        settings.config.setDeployment(model)
        secret?.let { settings.config.setSignerFromSecret(it.toPath()) }
        return settings.config.client(clientProvider)
    }

    final override fun run() {
        val chainsToDeploy = blockchain ?: settings.model.blockchains.keys
        val cliEnv = CliktCliEnv(this@AbstractDeploymentCommand)
        val res = ChromiaCompileApi.build(cliEnv,
                settings.model.filterBlockchains(chainsToDeploy))
                .onEach { it.filterGtxModules(cliEnv).validate() }
                .apply { validateRellVersion(client) }
                .apply { preDeploymentVerification(this) }
                .let { performDeploymentOperation(it) }
                .apply { afterDeployment(this) }
                .apply { save(settings.targetDir.toPath(), prefix = target, suffix = Clock.systemUTC().instant().toString()) }

        if (!res.isSuccess()) {
            throw ProgramResult(1)
        }
    }

    abstract fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>)

    abstract fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult>

    abstract fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>)

    protected fun validateRellVersion(client: PostchainClient) {
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
