package com.chromia.cli

import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import de.m3y.kformat.Table
import de.m3y.kformat.table
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.defaultHttpHandler
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl
import org.http4k.core.HttpHandler

class DeployInfoCommand(
        private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion,
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { Companion.httpHandlerFactory(it) }
) : CliktCommand(
        name = "info",
        help = "Information about any deployed blockchain"
) {

    private val settings by settingsOptionNotRequired()
    private val configuredOptions by ConfiguredDeploymentInfoOption {
        settings?.model ?: settingsOptionDefault().model
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption().cooccurring()
    private val verbose by option(help = "Show verbose information about nodes").flag()

    override fun run() {
        val option = configuredOptions ?: manualOptions!!
        val config = PostchainClientConfig(option.brid, endpointPool = EndpointPool.default(option.urls))
        val postchainClient = clientProvider.createClient(config)

        val clusterManagement = clusterManagementFactory.buildClusterManagement(postchainClient)
        val nodeStatusFinder = NodeStatusFinder(httpHandlerFactory(config), clientProvider, config, clusterManagement, verbose)

        try {
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header("Blockchain", "Rid", "Cluster")
                row(option.blockchainName, option.brid.toShortHex(), clusterManagement.getClusterOfBlockchain(option.brid))
            }.render().also { echo(it) }
            val clusterUrls = clusterManagement.getBlockchainApiUrls(option.brid)
            table {
                hints {
                    defaultAlignment = Table.Hints.Alignment.LEFT
                    borderStyle = Table.BorderStyle.SINGLE_LINE
                }
                header(nodeStatusFinder.tableHeaders())
                clusterUrls.forEach { url ->
                    val result = nodeStatusFinder.findStatus(Endpoint(url), option.brid)
                    row(*result.values())
                }
            }.render().also { echo(it) }
        } catch (e: ClientError) {
            echo("Cluster not found for blockchain rid ${option.brid.toShortHex()}")
        }
    }

    companion object : ClusterManagementFactory {
        fun httpHandlerFactory(config: PostchainClientConfig) = defaultHttpHandler(config)
        override fun buildClusterManagement(client: PostchainClient) = ClusterManagementImpl(client)
    }
}
