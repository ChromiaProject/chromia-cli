package com.chromia.cli.util

import com.chromia.cli.model.ChromiaCliModel
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.BlockchainRid

sealed class DeploymentOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val url: String
}

class RemoteDeploymentOption(private val settings: () -> ChromiaCliModel) : DeploymentOption("Deployment", help = "Make query towards a configured deployment") {
    private val name by option(help = "Name of deployment target").required()
    private val blockchain by option(help = "Name of blockchain in deployment configuration").required()

    override val brid: BlockchainRid
        get() {
            val deploymentModel = settings().deployments[name]
            require(deploymentModel != null) { "Deployment named $name not found in configuration" }
            val blockchainRid = deploymentModel.chains[blockchain]
            require(blockchainRid != null ) { "Blockchain named $blockchain not found in deployment configuration"}
            return blockchainRid
        }

    override val url: String
        get() {
            val deploymentModel = settings().deployments[name]
            require(deploymentModel != null) { "Deployment named $name not found in configuration" }
            return deploymentModel.apiUrl.joinToString(",")
        }
}

class LocalDeploymentOption : DeploymentOption("Node", help = "Make query/tx towards a test node") {
    private val blockchainRid by option(help = "Target Blockchain Rid").required()
    private val apiUrl by option(help = "Target api url").default("http://localhost:7740")

    override val brid: BlockchainRid
        get() = BlockchainRid.buildFromHex(blockchainRid)

    override val url: String
        get() = apiUrl
}
