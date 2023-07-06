package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.chromia.cli.versionfinder.Http4kRellVersionFinder
import com.chromia.cli.versionfinder.RellDeployVersionException
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid
import org.http4k.core.HttpHandler

class DeployCreateCommand(
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { DeployInfoCommand.httpHandlerFactory(it) },
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : AbstractDeploymentCommand(name = "create", help = "Deploy blockchain into container", clientProvider) {
    val confirm by option("-y", help = "Confirm that this will create a new deployment").flag()

    override fun beforeDeployment(deployedChains: Collection<String>) {
        val httpClient = httpHandlerFactory(createClientConfig())
        val rellVersionController = Http4kRellVersionFinder(httpClient)
        val targetVersion = rellVersionController.getTargetVersion(Endpoint(deployModel.urls.first()), deployModel.blockchainRid)

        if (targetVersion < settings.compile.langVersion) {
            throw RellDeployVersionException(settings.compile.rellVersion, targetVersion)
        }

        deployedChains.forEach { name ->
            if (deployModel.chains.containsKey(name)) throw PrintMessage("Blockchain $name is already deployed to network $target")
            if (!confirm) confirm(
                    "This will create a new deployment of $name on network $target. Would you like to create a new deployment?",
                    default = false
            )
        }
    }

    override fun afterDeployment(deployedChains: List<Pair<String, BlockchainRid>>) {
        echo("""
            Add the following to your project settings file:
            deployments:
              $target:
                chains:
                  ${deployedChains.joinToString("\n      ") { "${it.first}: x\"${it.second.toHex()}\"" }}
            """.trimIndent())
    }

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult) {
        BlockchainOperations(client.apiVersion, this)
                .newBlockchainOperation(clientConfig.pubkey.data, configHolder.configByteArray, configHolder.name, deployModel.container!!)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
