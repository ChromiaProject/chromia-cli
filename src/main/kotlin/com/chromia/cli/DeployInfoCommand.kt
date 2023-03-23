package com.chromia.cli

import com.chromia.cli.model.DeploymentModel
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.HeightFinder
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import de.m3y.kformat.Table
import de.m3y.kformat.table
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid

class DeployInfoCommand(
        private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion
): CliktCommand(
        name = "info",
        help = "Information about deployed blockchain"
) {

    private val settings by settingsOption()
    private val target by deployTargetOption().required().validate {
        val deployment = settings.deployments[it]
        require(deployment != null) { "Deployment $it not found" }
    }
    private val network: DeploymentModel get() = settings.deployments[target]!!
    private val blockchain by option(help = "Name of blockchain to deploy").required().validate {
        require(settings.deployments[target]!!.chains[it] != null) { "Blockchain $it not found" }
    }
    private val brid: BlockchainRid get() = network.chains[blockchain]!!

    override fun run() {
        val config = PostchainClientConfig(network.blockchainRid, endpointPool = EndpointPool.default(network.urls))
        val postchainClient = clientProvider.createClient(config)

        val clusterManagement = clusterManagementFactory.buildClusterManagement(postchainClient)

        try {
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Blockchain", "Rid", "Cluster")
                row(blockchain, brid.toShortHex(), clusterManagement.getClusterOfBlockchain(brid))
            }.render().also { echo(it) }
            val clusterUrls = clusterManagement.getBlockchainApiUrls(brid)
            val heightFinder = HeightFinder(clientProvider, config, clusterManagement)
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Node url", "Height", "Status")
                clusterUrls.forEach { url ->
                    val height = heightFinder.findHeight(Endpoint(url), brid)
                    row(url, height.height.toString(), height.status)
                }
            }.render().also { echo(it) }
        } catch (e: ClientError) {
            echo("Cluster not found for blockchain rid ${brid.toShortHex()}")
        }
    }

    companion object: ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainClient) = ClusterManagementImpl(client)
    }
}
