package com.chromia.cli

import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.exception.RellDeployVersionException
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.RellVersionController
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import org.http4k.core.HttpHandler

class DeployCreateCommand(
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { DeployInfoCommand.httpHandlerFactory(it) },
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy blockchain into container", clientProvider) {

    override fun beforeDeployment(deployedChains: Collection<BlockchainConfigHolder>) {
        val httpClient = httpHandlerFactory(createClientConfig())
        val rellVersionController = RellVersionController(httpClient)
        val targetVersion = rellVersionController.getTargetVersion(Endpoint(deployModel.urls.first()), deployModel.blockchainRid)

        if (targetVersion != settings.compile.rellVersion) {
            throw RellDeployVersionException(settings.compile.rellVersion, targetVersion)
        }

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
                  ${deployedChains.joinToString("\n      ") { "${it.name}: x\"${it.brid}\"" }}
            """.trimIndent())
    }

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: BlockchainConfigHolder) {
        BlockchainOperations(client.apiVersion, this)
                .newBlockchainOperation(clientConfig.pubkey.data, configHolder.configByteArray, configHolder.name, deployModel.container!!)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
