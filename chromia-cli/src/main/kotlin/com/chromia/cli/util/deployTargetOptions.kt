package com.chromia.cli.util

import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.config.ChromiaClientConfig.Companion.DEFAULT_API_URL
import com.chromia.cli.command.deployment.DeployInfoCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.blockchain.BridFetcher
import com.chromia.cli.tools.config.blockchainRidOption
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.defaultHttpHandler
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import org.http4k.core.HttpHandler

sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val blockchain: String
    abstract val brid: BlockchainRid
    abstract val url: String
    abstract fun createClient(config: ChromiaClientConfig): PostchainClient
    abstract fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient
}

class RemoteDeploymentOption(private val settings: () -> ChromiaModel) : DeploymentOption("Deployment", help = "Use a configured deployment") {
    private val network by deployTargetOption()
    override val blockchain by blockchainOption(help = "Name of blockchain in deployment configuration").required()
    override val brid: BlockchainRid
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            val blockchainRid = deploymentModel.chains[blockchain]
            require(blockchainRid != null) { "Blockchain named $blockchain not found in deployment configuration" }
            return blockchainRid
        }

    override val url: String
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.urls.joinToString(",")
        }

    override fun createClient(config: ChromiaClientConfig): PostchainClient {
        val directoryChain = createDirectoryClient(config)
        return PostchainClientImpl(directoryChain.config.copy(brid, EndpointPool.default(directoryChain.cmGetBlockchainApiUrls(brid))))
    }

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        return config.setBrid(settings().deployments[network]!!.blockchainRid).client(PostchainClientProviderImpl())
    }
}

class DeployedNetworkOption(private val settings: () -> ChromiaModel) : DeploymentOption("Deployment", help = "Use a configured deployment network") {
    private val network by deployTargetOption().required()
    override val blockchain: String
        get() = brid.toHex()
    override val brid: BlockchainRid
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.blockchainRid
        }

    override val url: String
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.urls.joinToString(",")
        }

    override fun createClient(config: ChromiaClientConfig): PostchainClient {
        val directoryChain = createDirectoryClient(config)
        return PostchainClientImpl(directoryChain.config.copy(brid, EndpointPool.default(directoryChain.cmGetBlockchainApiUrls(brid))))
    }

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        return config.setBrid(settings().deployments[network]!!.blockchainRid).client(PostchainClientProviderImpl())
    }
}

class LocalDeploymentOption(
        private val config: () -> ChromiaClientConfig,
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { defaultHttpHandler(it) }) : DeploymentOption("Node", help = "Target a test node") {
    private val blockchainRid by blockchainRidOption(help = "Target Blockchain RID")
    private val cid by option(help = "Target Blockchain IID").int()
    private val apiUrl by option(help = "Target api url").default(DEFAULT_API_URL)

    override val url
        get() = if (apiUrl != DEFAULT_API_URL) {
            apiUrl
        } else {
            config().apiUrls
        }
    override val blockchain: String
        get() = brid.toHex()

    override val brid
        // 1. explicit brid
        // 2. explicit cid
        // 3. config (local/global)
        // 4. cid = 0
        get(): BlockchainRid {
            if (blockchainRid != null) return BlockchainRid.buildFromHex(blockchainRid!!)
            if (cid != null) return blockchainRidFromIid(cid!!)
            val bridFromConfig = config().blockchainRid
            if (bridFromConfig != BlockchainRid.ZERO_RID) return bridFromConfig
            return blockchainRidFromIid(0)
        }

    private fun blockchainRidFromIid(cid: Int) = BridFetcher(httpHandlerFactory(PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(url))), url).fetchBlockchainRid(cid)

    override fun createClient(config: ChromiaClientConfig) = config.client(PostchainClientProviderImpl())
    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val directoryBrid = BridFetcher(httpHandlerFactory(PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(listOf(url)))), url)
                .fetchBlockchainRid(0)
        return config.setBrid(directoryBrid).client(PostchainClientProviderImpl())
    }
}
