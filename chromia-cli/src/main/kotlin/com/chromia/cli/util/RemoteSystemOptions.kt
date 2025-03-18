package com.chromia.cli.util

import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.blockchainRidOption
import com.chromia.cli.tools.config.targetUrlOption
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.core.PostchainClient
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.BlockchainRid

sealed class SystemOption(name: String, help: String?) : OptionGroup(name, help) {
    abstract val containerId: String?
    abstract val client: PostchainClient
}


class RemoteSystemOption(private val settings: () -> Pair<ChromiaModel, ChromiaClientConfig>) : SystemOption("Chromia Configuration", "Use connection configured under deployment from chromia.yml") {
    private val network by deployTargetOption().required()

    override val client: PostchainClient by lazy {
        val (model, config) = settings()
        val deploymentModel = model.deployments[network]
        require(deploymentModel != null) { "Deployment named $network not found in configuration" }
        config.setDeployment(deploymentModel)
        config.client(PostchainClientProviderImpl())
    }

    override val containerId: String?
        get() {
            val deploymentModel = settings().first.deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.container
        }
}

class ExplicitRemoteSystemOption(config: () -> ChromiaClientConfig) : SystemOption("Manual Configuration", "Set connection parameters manually") {
    private val targetBrid by blockchainRidOption(help = "Target Blockchain RID").required()

    val url: String by targetUrlOption().required()

    val brid get() = BlockchainRid.buildFromHex(targetBrid)
    override val client: PostchainClient by lazy {
        config().setBrid(brid).setApiUrls(listOf(url)).client(PostchainClientProviderImpl())
    }

    override val containerId: String? = null
}
