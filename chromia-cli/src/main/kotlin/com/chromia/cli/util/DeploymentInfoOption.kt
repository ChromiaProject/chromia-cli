package com.chromia.cli.util

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.DeploymentModel
import com.chromia.cli.tools.blockchain.BridFetcher
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaClientProvider
import org.http4k.core.HttpHandler

sealed class DeploymentInfoOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val blockchainName: String
    abstract val networkBrid: BlockchainRid
    abstract val urls: List<String>
    abstract fun networkClient(): PostchainClient
    abstract fun blockchainClient(): PostchainClient
}

class ConfiguredDeploymentInfoOption(private val clientProvider: PostchainClientProvider, private val settings: () -> ChromiaModel) : DeploymentInfoOption("Chromia Configuration", help = "Use connection configured under deployment from chromia.yml") {
    private val target by deployTargetOption().required().validate {
        val deployment = settings().deployments[it]
        require(deployment != null) { "Deployment $it not found" }
    }
    private val blockchain by blockchainOption(help = "Name of blockchain to deploy").required()
            .validate {
                require(settings().deployments[target]!!.chains[it] != null) { "Blockchain $it not found" }
            }

    private val network: DeploymentModel get() = settings().deployments[target]!!
    override val brid: BlockchainRid get() = network.chains[blockchain]!!
    override val blockchainName: String get() = blockchain
    override val urls: List<String> get() = network.urls
    override val networkBrid: BlockchainRid get() = network.blockchainRid
    override fun networkClient(): PostchainClient {
        val config = PostchainClientConfig(networkBrid, EndpointPool.default(urls))
        return clientProvider.createClient(config)
    }

    override fun blockchainClient() =
            ChromiaClientProvider(ClusterManagementImpl(networkClient()), networkClient().config)
                    .blockchain(brid)
}

class ManualDeploymentInfoOption(private val clientProvider: PostchainClientProvider, private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler) : DeploymentInfoOption("Manual", help = "Set connection parameters manually") {
    private val blockchainRid by blockchainRidOption("Target Blockchain RID").required()
    private val url by option(help = "Target url").multiple().validate { it.isNotEmpty() }

    override val urls: List<String> get() = url
    override val brid: BlockchainRid get() = blockchainRid.let { BlockchainRid.buildFromHex(it) }
    override val blockchainName: String get() = blockchainRid
    override val networkBrid: BlockchainRid
        get() = BridFetcher(httpHandlerFactory(PostchainClientConfig(brid, EndpointPool.default(urls))), url.first())
                .fetchBlockchainRid(0)

    override fun networkClient(): PostchainClient {
        val config = PostchainClientConfig(networkBrid, EndpointPool.default(urls))
        return clientProvider.createClient(config)
    }

    override fun blockchainClient(): PostchainClient {
        val config = PostchainClientConfig(brid, EndpointPool.default(urls))
        return clientProvider.createClient(config)
    }
}
