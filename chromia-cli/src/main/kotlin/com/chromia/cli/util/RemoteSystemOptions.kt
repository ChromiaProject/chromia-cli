package com.chromia.cli.util

import com.chromia.cli.model.ChromiaModel
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.BlockchainRid

sealed class SystemOption(name: String, help: String?) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val url: String
    abstract val containerId: String?
}


class RemoteSystemOption(private val settings: () -> ChromiaModel) : SystemOption("Chromia Configuration", "Use connection configured under deployment from chromia.yml") {
    private val network by deployTargetOption().required()

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

    override val containerId: String?
        get() {
            val deploymentModel = settings().deployments[network]
            require(deploymentModel != null) { "Deployment named $network not found in configuration" }
            return deploymentModel.container
        }
}

class ExplicitRemoteSystemOption() : SystemOption("Manual Configuration", "Set connection parameters manually") {
    private val targetBrid by blockchainRidOption(help = "Target Blockchain RID").required()

    override val url: String by targetUrlOption().required()

    override val brid get() = BlockchainRid.buildFromHex(targetBrid)

    override val containerId: String? = null
}
