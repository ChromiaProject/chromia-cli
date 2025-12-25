package com.chromia.cli.util

import com.chromia.build.tools.blockchain.BridFetcher
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.config.ChromiaClientConfig.Companion.DEFAULT_API_URL
import com.chromia.build.tools.config.DEVNET1
import com.chromia.build.tools.config.DEVNET2
import com.chromia.build.tools.config.MAINNET
import com.chromia.build.tools.config.TESTNET
import com.chromia.build.tools.config.getProviderUrlsForNetwork
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.blockchainRidOption
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.switch
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.defaultHttpHandler
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.StandardChromiaClient
import org.http4k.core.HttpHandler

sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val blockchain: String
    abstract val brid: BlockchainRid
    abstract val urls: List<String>
    abstract fun createClient(config: ChromiaClientConfig): PostchainClient
    abstract fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient

    protected fun getUrls(settings: ChromiaModel, network: String): List<String> {
        val urls = settings.deployments[network]?.urls
                ?.takeIf { it.isNotEmpty() }
                ?: getProviderUrlsForNetwork(network)
                ?: emptyList()
        require(urls.isNotEmpty()) { "No urls found for network $network" }
        return urls
    }

    protected fun createClientFromDirectoryChain(config: ChromiaClientConfig): PostchainClient {
        val directoryChain = createDirectoryClient(config)
        val apiUrls = directoryChain.cmGetBlockchainApiUrls(brid)
        require(apiUrls.isNotEmpty()) {
            "No API URLs found for brid '$brid'. " +
                "Check if the directory chain is correctly configured and that '$brid' correct."
        }
        val updatedConfig = directoryChain.config.copy(
            blockchainRid = brid,
            endpointPool = EndpointPool.default(apiUrls),
            signers = config.signers)
        return PostchainClientImpl(updatedConfig)
    }

    protected fun createDirectoryClientFromBrid(
        config: ChromiaClientConfig,
        d1BridFromModel: BlockchainRid?
    ): PostchainClient {
        if (d1BridFromModel != null) {
            return config.setBrid(d1BridFromModel).client(PostchainClientProviderImpl())
        }
        val updatedConfig =
                PostchainClientConfig.defaultConfig.copy(
                signers = config.signers,
                endpointPool = EndpointPool.default(urls)
            )
        return StandardChromiaClient(updatedConfig)
                .getDirectoryChainClient()
    }

    protected fun fetchBridFromChainID(
        cid: Int = 0,
        httpHandler: HttpHandler = defaultHttpHandler(PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(urls)))
    ): BlockchainRid {
        // TODO: make BridFetcher takes multiple URLS
        try {
            return BridFetcher(httpHandler, urls.first()).fetchBlockchainRid(cid)
        } catch (e: Exception) {
            throw CliktError(e.message)
        }
    }
}

class RemoteDeploymentOption(private val settings: () -> ChromiaModel) : DeploymentOption(
        "Deployment", help = "Use a configured deployment network target in chromia.yml"
) {
    val network by deployTargetOption().required()
            .validate { require(settings().deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    private val blockchainOptionValue by blockchainOption(help = "Name of blockchain in deployment configuration")

    override val blockchain: String
        get() = blockchainOptionValue
            ?: inferBlockchainFromModel()
            ?: createBlockchainSelectionError()

    private fun createBlockchainSelectionError(): Nothing =
        deploymentConfig.chains.keys.let { availableChains ->
            val message = when {
                availableChains.isEmpty() -> createNoBlockchainsMessage()
                else -> createMultipleBlockchainsMessage(availableChains)
            }
            throw PrintMessage(message, 1)
        }

    private fun createNoBlockchainsMessage(): String = """
    |No blockchains configured for deployment '$network'
    |Resolution options:
    |- Add blockchain configurations to your deployment
    |- Choose a different deployment target using --network
    """.trimMargin()

    private fun createMultipleBlockchainsMessage(availableChains: Set<String>): String = """
    |Multiple blockchains available in deployment '$network'
    |Available chains: ${availableChains.joinToString()}
    |Resolution:
    |- Specify the target blockchain using: --blockchain <name>
    |    - Example: --blockchain ${availableChains.first()}
    """.trimMargin()

    private fun inferBlockchainFromModel(): String? =
        settings().deployments[network]?.chains?.keys?.singleOrNull()

    private val deploymentConfig by lazy {
        settings().deployments[network]
            ?: error("Deployment named $network not found in configuration")
    }

    override val brid: BlockchainRid get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            val blockchainRid = deploymentModel.chains[blockchain]
            require(blockchainRid != null) { "Blockchain named $blockchain not found in deployment configuration" }
            return blockchainRid
        }

    override val urls: List<String> get() = getUrls(settings(), network)

    override fun createClient(config: ChromiaClientConfig) = createClientFromDirectoryChain(config)

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val d1BridFromModel = settings().deployments[network]!!.blockchainRid
        return createDirectoryClientFromBrid(config, d1BridFromModel)
    }
}

class DeployedNetworkOption(private val settings: () -> ChromiaModel) : DeploymentOption(
        "Network target options",
        help = "Use a configured deployment network target in project settings file"
) {
    val network by deployTargetOption().required()
            .validate { require(settings().deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    override val blockchain: String
        get() = brid.toHex()

    override val brid: BlockchainRid
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            // TODO: Might be better to use StandardChromiaClient.getDirectoryClient() instead of fetchBridFromChainID()
            return deploymentModel.blockchainRid ?: fetchBridFromChainID()
        }

    override val urls: List<String> get() = getUrls(settings(), network)

    override fun createClient(config: ChromiaClientConfig) = createClientFromDirectoryChain(config)

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val d1BridFromModel = settings().deployments[network]?.blockchainRid
        return createDirectoryClientFromBrid(config, d1BridFromModel)
    }
}


class ExplicitDeploymentOption(
    private val config: () -> ChromiaClientConfig,
    private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { defaultHttpHandler(it) }
) : DeploymentOption("dApp target options") {

    private val blockchainRid by blockchainRidOption(help = "Target Blockchain RID")
    private val cid by option(help = "Target Blockchain IID").int()

    // TODO: Is this default api url needed? Seems as if we don't use it within this option group
    private val apiUrl by option(help = "Target api url").default(DEFAULT_API_URL)

    val chromiaNetwork by option(help = "Select network, use instead of --api-url").switch(
        "--$MAINNET" to MAINNET,
        "--$TESTNET" to TESTNET,
    )

    val chromiaDevNetwork by option(hidden = true).switch(
            "--$DEVNET1" to DEVNET1,
            "--$DEVNET2" to DEVNET2,
    )

    override val urls get(): List<String> {
        if (apiUrl != DEFAULT_API_URL) return listOf(apiUrl)
        return chromiaNetwork?.let(::getProviderUrlsForNetwork)
            ?: chromiaDevNetwork?.let(::getProviderUrlsForNetwork)
            ?: config().apiUrls
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

    override fun createClient(config: ChromiaClientConfig): PostchainClient {
        return if (config.endpointPool.size == 1) {
            config.client(PostchainClientProviderImpl())
        } else {
            val updatedConfig =
                PostchainClientConfig.defaultConfig.copy(
                    signers = config.signers,
                    endpointPool = config.endpointPool
                )
            StandardChromiaClient(updatedConfig).getClient(brid)
        }
    }

    override fun createDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        return if (!apiUrl.contains("http://localhost:")) {
            val updatedConfig =
                    PostchainClientConfig.defaultConfig.copy(
                            signers = config.signers,
                            endpointPool = config.endpointPool
                    )
            StandardChromiaClient(updatedConfig).getDirectoryChainClient()
        } else {
            createLocalDirectoryClient(config)
        }
    }

    private fun createLocalDirectoryClient(config: ChromiaClientConfig): PostchainClient {
        val initialConfig = PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.default(urls))
        val httpHandler = httpHandlerFactory(initialConfig)
        val directoryBrid = fetchBridFromChainID(httpHandler = httpHandler)
        return config.setBrid(directoryBrid).client(PostchainClientProviderImpl())
    }
}
