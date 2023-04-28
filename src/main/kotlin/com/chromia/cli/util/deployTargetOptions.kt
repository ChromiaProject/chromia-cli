package com.chromia.cli.util

import com.chromia.cli.model.ChromiaCliModel
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.int
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaClientProvider
import java.net.ConnectException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val url: String
    abstract fun createClient(config: PostchainClientConfig): PostchainClient
}

class RemoteDeploymentOption(private val settings: () -> ChromiaCliModel) : DeploymentOption("Deployment", help = "Make query towards a configured deployment") {
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

class LocalDeploymentOption : DeploymentOption("Node", help = "Make query/tx towards a test node") {
    private val blockchainRid by blockchainRidOption(help = "Target Blockchain RID")
    private val cid by option(help = "Target Blockchain IID").int().default(0)
    private val apiUrl by option(help = "Target api url").default("http://localhost:7740")

    override val url get() = apiUrl
    override val brid get() = blockchainRid?.let { BlockchainRid.buildFromHex(it) } ?: blockchainRidFromIid()

    private fun blockchainRidFromIid() = BridFinder(url).findBlockchainRid(cid)

    override fun createClient(config: PostchainClientConfig) = PostchainClientImpl(config)
}
