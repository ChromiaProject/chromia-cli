package com.chromia.cli.util

import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.model.DeploymentModel
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.common.BlockchainRid

sealed class DeploymentInfoOption(name: String, help: String? = null) : OptionGroup(name, help) {
    abstract val brid: BlockchainRid
    abstract val blockchainName: String
    abstract val urls: List<String>
}

class ConfiguredDeploymentInfoOption(private val settings: () -> ChromiaCliModel) : DeploymentInfoOption("Configured", help = "Information about a deployed blockchain") {
    private val target by option("--target", help = "If a specific target deploy model should be used").required().validate {
        val deployment = settings().deployments[it]
        require(deployment != null) { "Deployment $it not found" }
    }
    private val blockchain by option(help = "Name of blockchain to deploy").required().validate {
        require(settings().deployments[target]!!.chains[it] != null) { "Blockchain $it not found" }
    }

    private val network: DeploymentModel get() = settings().deployments[target]!!

    override val brid: BlockchainRid
        get() {
            return network.chains[blockchain]!!
        }

    override val blockchainName: String
        get() = blockchain

    override val urls: List<String>
        get() {
            return network.urls
        }
}

class ManualDeploymentInfoOption : DeploymentInfoOption("Manual", help = "Information about a deployed blockchain that is not in the settings file") {
    private val blockchainRid by option("-brid", "--blockchain-rid", help = "Target Blockchain RID").required()
    private val url by option(help = "Target url").multiple()

    override val urls: List<String>
        get() = url

    override val brid: BlockchainRid
        get() = blockchainRid.let { BlockchainRid.buildFromHex(it) }

    override val blockchainName: String
        get() = blockchainRid
}
