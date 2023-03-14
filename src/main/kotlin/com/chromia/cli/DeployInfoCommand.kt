package com.chromia.cli

import com.chromia.cli.model.DeploymentModel
import com.chromia.cli.util.NodeStatusChecker
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
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid
import net.postchain.d1.client.ChromiaClientProvider
import net.postchain.d1.cluster.ClusterManagement
import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler

class DeployInfoCommand(private val client: HttpHandler? = null, private val clusterManagement: ClusterManagement? = null): CliktCommand(
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
        val clientProvider = fromClientConfig(config, client, clusterManagement)

        try {
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Blockchain", "Rid", "Cluster")
                row(blockchain, brid.toShortHex(), clientProvider.clusterManagement.getClusterOfBlockchain(brid))
            }.render().also { echo(it) }
            val clusterUrls = clientProvider.clusterManagement.getBlockchainApiUrls(brid)
            val statusChecker = NodeStatusChecker(brid, client ?: ApacheClient())
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Node url", "Status")
                clusterUrls.forEach { url ->
                    when (val result = statusChecker.checkStatus(url)) {
                        is NodeStatusChecker.NodeStatus.Status -> row(url, "Height: ${result.height}")
                        is NodeStatusChecker.NodeStatus.Error -> row(url, result.error)
                    }
                }
            }.render().also { echo(it) }
        } catch (e: ClientError) {
            echo("Cluster not found for blockchain rid ${brid.toShortHex()}")
        }
    }

    companion object {
        /**
         * Builds an instance of [ChromiaClientProvider] that uses a http client to query chain0
         */
        fun fromClientConfig(config: PostchainClientConfig, httpHandler: HttpHandler?, clusterManagement: ClusterManagement?): ChromiaClientProvider {
            val chain0Client: PostchainClient = httpHandler?.let { PostchainClientImpl(config, it) } ?: PostchainClientImpl(config)
            val clusterManagement = clusterManagement ?: ClusterManagementImpl(chain0Client)
            return ChromiaClientProvider(config.failOverConfig, clusterManagement)
        }
    }
}
