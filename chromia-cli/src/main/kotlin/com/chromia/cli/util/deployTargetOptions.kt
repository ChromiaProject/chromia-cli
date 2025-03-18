package com.chromia.cli.util

import com.chromia.build.tools.blockchain.BridFetcher
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.config.ChromiaClientConfig.Companion.DEFAULT_API_URL
import com.chromia.cli.model.ChromiaModel
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
    abstract val urls: List<String>
    abstract fun createClient(config: ChromiaClientConfig): PostchainClient
    abstract fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient
    
    protected fun fetchBridFromChainID(
            cid: Int = 0,
            httpHandler: HttpHandler = defaultHttpHandler(PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(urls)))
    ): BlockchainRid {
        // TODO: make BridFetcher takes multiple URLS
        return BridFetcher(httpHandler, urls.first()).fetchBlockchainRid(cid)
    }
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

    override val urls: List<String>
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.urls
        }

    override fun createClient(config: ChromiaClientConfig): PostchainClient {
        val directoryChain = createDirectoryClient(config)
        return PostchainClientImpl(directoryChain.config.copy(brid, EndpointPool.default(directoryChain.cmGetBlockchainApiUrls(brid))))
    }

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val d1BridFromModel = settings().deployments[network]!!.blockchainRid
        if (d1BridFromModel != null) {
            return config.setBrid(d1BridFromModel).client(PostchainClientProviderImpl())
        }
        val d1BridFetched = fetchBridFromChainID()
        return config.setBrid(d1BridFetched).client(PostchainClientProviderImpl())
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

            return if(deploymentModel.blockchainRid != null) {
                deploymentModel.blockchainRid
            } else {
                fetchBridFromChainID()
            }
        }

    override val urls: List<String>
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.urls
        }

    override fun createClient(config: ChromiaClientConfig): PostchainClient {
        val directoryChain = createDirectoryClient(config)
        return PostchainClientImpl(directoryChain.config.copy(brid, EndpointPool.default(directoryChain.cmGetBlockchainApiUrls(brid))))
    }

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val d1BridFromModel = settings().deployments[network]!!.blockchainRid
        if (d1BridFromModel != null) {
            return config.setBrid(d1BridFromModel).client(PostchainClientProviderImpl())
        }
        val d1BridFetched = fetchBridFromChainID()
        return config.setBrid(d1BridFetched).client(PostchainClientProviderImpl())
    }

    private fun List<String>.isDefaultUrl() = this.size == 1 && this[0] == DEFAULT_API_URL
}

class LocalDeploymentOption(
        private val config: () -> ChromiaClientConfig,
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { defaultHttpHandler(it) }) : DeploymentOption("Node", help = "Target a test node") {
    private val blockchainRid by blockchainRidOption(help = "Target Blockchain RID")
    private val cid by option(help = "Target Blockchain IID").int()
    private val apiUrl by option(help = "Target api url").default(DEFAULT_API_URL)

    override val urls: List<String>
        get() = if (apiUrl != DEFAULT_API_URL) {
            listOf(apiUrl)
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
            val initialConfig = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(urls))
            val httpHandler = httpHandlerFactory(initialConfig)
            return when {
                blockchainRid != null -> BlockchainRid.buildFromHex(blockchainRid!!)
                cid != null -> fetchBridFromChainID(cid!!, httpHandler)
                config().blockchainRid != BlockchainRid.ZERO_RID -> config().blockchainRid
                else -> fetchBridFromChainID(httpHandler = httpHandler)
            }
        }


    override fun createClient(config: ChromiaClientConfig) = config.client(PostchainClientProviderImpl())

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val initialConfig = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(urls))
        val httpHandler = httpHandlerFactory(initialConfig)
        val directoryBrid = fetchBridFromChainID(httpHandler = httpHandler)
        return config.setBrid(directoryBrid).client(PostchainClientProviderImpl())
    }
}
