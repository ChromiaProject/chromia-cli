package com.chromia.cli

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.ConfiguredDeploymentInfoOption
import com.chromia.cli.util.ManualDeploymentInfoOption
import com.chromia.cli.util.NodeStatusFinder
import com.chromia.cli.util.settingsOptionDefault
import com.chromia.cli.util.settingsOptionNotRequired
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.defaultHttpHandler
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import net.postchain.cm.cm_api.ClusterManagementImpl
import org.http4k.core.HttpHandler

class DeployInfoCommand(
        private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion,
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { Companion.httpHandlerFactory(it) }
) : CliktCommand(
        name = "info",
        help = "Information about a deployed blockchain"
) {

    private val settings by settingsOptionNotRequired()
    private val configuredOptions by ConfiguredDeploymentInfoOption(clientProvider) {
        settings?.model ?: settingsOptionDefault().model
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption(clientProvider, httpHandlerFactory).cooccurring()
    private val verbose by option(help = "Show verbose information about nodes").flag()
    private val option by lazy { configuredOptions ?: manualOptions ?: throw PrintMessage("No target blockchain to analyze specified") }

    override fun run() {
        val networkClient = option.networkClient()
        val config = networkClient.config

        val clusterManagement = clusterManagementFactory.buildClusterManagement(networkClient)
        val nodeStatusFinder = NodeStatusFinder(httpHandlerFactory(config), clientProvider, config, clusterManagement, verbose)

        try {
            echo(defaultTable {
                header {
                    row("Blockchain", "Rid", "Cluster")
                }
                body {
                    row(option.blockchainName, option.brid.toShortHex(), clusterManagement.getClusterOfBlockchain(option.brid))
                }
            })
            val clusterUrls = clusterManagement.getBlockchainApiUrls(option.brid)
            echo(defaultTable {
                header{
                    row(*nodeStatusFinder.tableHeaders().toTypedArray())
                }
                body {
                    clusterUrls.forEach { url ->
                        val result = nodeStatusFinder.findStatus(Endpoint(url), option.brid)
                        row(*result.values())
                    }
                }
            })
        } catch (e: ClientError) {
            echo("Cluster not found for blockchain rid ${option.brid.toShortHex()}")
            echo(e.message)
        }
    }

    companion object : ClusterManagementFactory {
        fun httpHandlerFactory(config: PostchainClientConfig) = defaultHttpHandler(config)
        override fun buildClusterManagement(client: PostchainQuery) = ClusterManagementImpl(client)
    }
}
