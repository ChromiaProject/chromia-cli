package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.api.result.isSuccess
import com.chromia.api.result.save
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.util.getFormattedUtcDateTime
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.LibraryRidResolver
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.keepOnlyStandardGtxModules
import com.chromia.cli.versionfinder.CanNotFindBlockchainException
import com.chromia.cli.versionfinder.NoNodeRunningContainerException
import com.chromia.cli.versionfinder.PostchainRellVersionFinder
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.chromia.directory1.common.queries.getClusterApiUrls
import com.chromia.directory1.common.queries.getContainerData
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.Endpoint
import net.postchain.gtv.GtvFactory.gtv

abstract class AbstractDeploymentCommand(name: String, help: String, protected val clientProvider: PostchainClientProvider) : ChromiaCommand(name = name, help = help) {

    protected val settings by chromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.model.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }
    protected val noCompression by option("--no-compression", help = "If compression on rell sources should not be done").flag()

    val networkTarget by DeployedNetworkOption { settings.model }

    private val hideLibWarnings by hideLibWarningsOption()

    protected val client by lazy {
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        clientConfig.configureSigners(keyPairSource)
        val client = networkTarget.createClient(clientConfig)
        if (client.config.signers.isEmpty()) {
            throw PrintMessage("To be able to deploy, you must specify signer keys. Either using --secret file or --key-id or in the config.", statusCode = 1)
        }
        client
    }

    protected val deployModel by lazy {
        settings.model.deployments[networkTarget.network].let { model ->
            var activeModel = model
            if (activeModel?.container == null) {
                throw PrintMessage("No container specified on network ${networkTarget.network}")
            }
            if (model.blockchainRid == null ) {
                activeModel = activeModel.copy(blockchainRid = networkTarget.brid)
            }
            if (model.urls.isEmpty()) {
                activeModel = activeModel.copy(url = gtv(networkTarget.urls.map { gtv(it) }))
            }
            activeModel
        }
    }

    final override fun run() {
        val cliEnv = BuildCliEnv(this, hideLibWarnings)
        val chainsToDeploy = blockchain ?: explicitChainsToDeploy()
        val chromiaModel = settings.model.filterBlockchains(chainsToDeploy)
        val res = ChromiaCompileApi.build(cliEnv,chromiaModel)
                .onEach { it.keepOnlyStandardGtxModules().validate() }
                .apply { validateRellVersion(client) }
                .apply { preDeploymentVerification(this) }
                .let { performDeploymentOperation(it) }
                .apply { afterDeployment(this) }
                .apply {
                    save(
                        settings.targetDir.toPath(),
                        prefix = networkTarget.network,
                        suffix = getFormattedUtcDateTime()
                    )
                }

        if (!res.isSuccess()) {
            throw ProgramResult(1)
        }
    }

    abstract fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>)

    abstract fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult>

    abstract fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>)

    abstract fun explicitChainsToDeploy(): Collection<String>

    protected fun validateRellVersion(client: PostchainClient) {
        val rellVersionController = PostchainRellVersionFinder(client.config, clientProvider)

        val clusterName = client.getContainerData(deployModel.container!!).cluster
        val clusterNodeUrls = client.getClusterApiUrls(clusterName)

        val targetVersions = clusterNodeUrls.mapNotNull {
            try {
                rellVersionController.getTargetVersion(Endpoint(it), deployModel.blockchainRid!!)
            } catch (e: CanNotFindBlockchainException) {
                throw e
            } catch (e: Exception) {
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
