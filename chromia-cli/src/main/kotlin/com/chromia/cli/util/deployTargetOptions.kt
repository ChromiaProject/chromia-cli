package com.chromia.cli.util

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.blockchain.BridFetcher
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.defaultHttpHandler
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaClientProvider
import org.http4k.core.HttpHandler

sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val url: String
    abstract fun createClient(config: PostchainClientConfig): PostchainClient
}

class RemoteDeploymentOption(private val settings: () -> ChromiaModel) : DeploymentOption("Deployment", help = "Make query towards a configured deployment") {
    private val network by deployTargetOption()
    private val blockchain by blockchainOption(help = "Name of blockchain in deployment configuration").required()
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

    override fun createClient(config: PostchainClientConfig) = ChromiaClientProvider.fromClientConfig(
            config.copy(blockchainRid = settings().deployments[network]!!.blockchainRid)
    ).blockchain(config.blockchainRid)
}

class LocalDeploymentOption(private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { defaultHttpHandler(it) }) : DeploymentOption("Node", help = "Make query/tx towards a test node") {
    private val blockchainRid by blockchainRidOption(help = "Target Blockchain RID")
    private val cid by option(help = "Target Blockchain IID").int().default(0)
    private val apiUrl by option(help = "Target api url").default("http://localhost:7740")

    override val url get() = apiUrl
    override val brid get() = blockchainRid?.let { BlockchainRid.buildFromHex(it) } ?: blockchainRidFromIid()

    private fun blockchainRidFromIid() = BridFetcher(httpHandlerFactory(PostchainClientConfig(BlockchainRid.ZERO_RID, EndpointPool.singleUrl(url))), url).fetchBlockchainRid(cid)

    override fun createClient(config: PostchainClientConfig) = PostchainClientImpl(config)
}
