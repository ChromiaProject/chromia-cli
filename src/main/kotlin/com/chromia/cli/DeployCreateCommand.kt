package com.chromia.cli

import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl

class DeployCreateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy blockchain into container", clientProvider) {

    override fun beforeDeployment(deployedChains: Collection<BlockchainConfigHolder>) {
        deployedChains.forEach { (name, _, _) ->
            if (deployModel.chains.containsKey(name)) throw PrintMessage("Blockchain $name is already deployed to network $target")
            confirm(
                    "This will create a new deployment of $name on network $target. Would you like to create a new deployment?",
                    default = false, abort = true)
        }
    }

    override fun afterDeployment(deployedChains: Collection<BlockchainConfigHolder>) {
        echo("""
            Add the following to your project settings file:
            deployments:
              $target:
                chains:
                  ${deployedChains.joinToString("\n      ") { "${it.name}: ${it.brid}" }}
            """.trimIndent())
    }

    override fun addDeploymentOperation(transactionBuilder: TransactionBuilder, client: PostchainClient, configHolder: BlockchainConfigHolder) {
        BlockchainOperations(client.apiVersion, transactionBuilder)
                .newBlockchainOperation(client.pubkey.data, configHolder.configByteArray, configHolder.name, deployModel.container!!)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainClient) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
