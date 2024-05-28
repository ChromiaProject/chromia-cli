package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl
import org.http4k.core.Status

class DeployUpdateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion,

        ) : AbstractDeploymentCommand(name = "update", help = "Update configuration of a deployed blockchain", clientProvider) {
    private val height by option(help = "Deploy configuration at a specific height").long().validate {
        require(blockchain?.size == 1 || deployModel.chains.size == 1) { "When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify" }
    }
    private val verifyOnly by option("--verify-only", help = "Verifies blockchain config without sending update transaction").flag()
    private val skipVerification by option("--skip-verification", help = "Skip verification of blockchain config before sending update transaction").flag()

    override fun preDeploymentVerification(compiledChains: Collection<BlockchainConfiguration>) {
        if (skipVerification) {
            echo("Skipping verification of blockchain config")
            return
        }
        val messages = compiledChains.mapNotNull { chain -> verifyConfiguration(chain, client) }
        if (messages.isNotEmpty()) {
            throw PrintMessage(messages.joinToString("\n"), statusCode = 1)
        }
        if (verifyOnly) throw PrintMessage("Verification only, skipping sending updates", 0)
    }

    override fun performDeploymentOperation(configurations: List<BlockchainConfiguration>): List<BlockchainDeploymentResult> {
        return ChromiaDeploymentApi.update(cliEnv(), deployModel, settings.config, configurations, !noCompression, height)
    }

    override fun afterDeployment(deployTxs: List<BlockchainDeploymentResult>) {
        for ((chain, _) in deployTxs) {
            echo("Blockchain ${chain.name} was successfully updated on network $target")
        }
    }

    override fun explicitChainsToDeploy(): Collection<String> {
        val chains = settings.model.deployments[target]?.chains?.keys
        if (chains.isNullOrEmpty()) {
            throw PrintMessage("No chains found in deployment $target", 1)
        }
        return chains
    }

    private fun verifyConfiguration(chain: BlockchainConfiguration, directoryChainClient: PostchainClient): String? {
        val blockchainRid = deployModel.chains[chain.name]
                ?: throw PrintMessage("Blockchain ${chain.name} cannot be updated since it has not been deployed to network $target. Specify target blockchain rid in chromia.yml")

        val clusterManagement = clusterManagementFactory.buildClusterManagement(directoryChainClient)
        val endpoint = EndpointPool.default(clusterManagement.getBlockchainApiUrls(blockchainRid).toList())
        val nodeClient = clientProvider.createClient(directoryChainClient.config.copy(blockchainRid, endpoint))
        try {
           nodeClient.validateConfiguration(chain.config)
            echo("Blockchain ${chain.name} was successfully verified against deployed chain on network $target")
        } catch (e: ClientError) {
            return when (e.status) {
                Status.UNAUTHORIZED -> "Node rejected request. You might need to update your chr to latest version. Reason: ${e.errorMessage}"
                Status.FORBIDDEN -> "You do not have access to validate configuration against this blockchain. Make sure the configured keypair match the blockchain provider/owner and try again."
                else -> "Blockchain ${chain.name} cannot be updated on network $target. Reason: ${e.errorMessage}"
            }
        }
        return null
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainQuery) = CliktClusterManagement(ClusterManagementImpl(client))

    }
}
