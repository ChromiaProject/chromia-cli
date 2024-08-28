package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.cm.cm_api.ClusterManagementImpl

class DeployCreateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy blockchain into container", clientProvider) {
    private val confirm by option("-y", help = "Confirm that this will create a new deployment").flag()

    override fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>) {
        compiledChains.forEach { chain ->
            if (deployModel.chains.containsKey(chain.name)) throw PrintMessage("Blockchain '${chain.name}' is already defined in the configuration file: '${settings.modelFile}' under the deployment: '$target'")
            if (!confirm && YesNoPrompt("This will create a new deployment of ${chain.name} on network $target. Would you like to create a new deployment?",
                            terminal, default = false
                    ).ask() != true) throw PrintMessage("Deployment was aborted")
        }
    }

    override fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult> {
        return ChromiaDeploymentApi.create(cliEnv(), deployModel, settings.config, configurations, !noCompression)
    }

    override fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>) {
        val successfulDeployments = deployTxs.filter { it.success && it.blockchainRid != null }
        if (successfulDeployments.isNotEmpty()) {
            echo("""
                Add the following to your project settings file:
                deployments:
                  $target:
                    chains:
                      ${successfulDeployments.joinToString("\n      ") { "${it.blockchain.name}: x\"${it.blockchainRid!!.toHex()}\"" }}
                """.trimIndent())
        }
    }

    override fun explicitChainsToDeploy(): Collection<String> {
        return settings.model.blockchains.keys
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
