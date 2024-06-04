package com.chromia.cli.command.deployment

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.tools.formatter.fixKey
import com.chromia.cli.tools.formatter.json
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.ConfiguredDeploymentInfoOption
import com.chromia.cli.util.ManualDeploymentInfoOption
import com.chromia.cli.util.NodeStatusFinder
import com.chromia.cli.util.TableOutputFormat
import com.chromia.cli.util.tableOutputFormat
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
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

    private val settings by optionalChromiaModelOption()
    private val configuredOptions by ConfiguredDeploymentInfoOption(clientProvider) {
        settings.model ?: ChromiaModel.default()
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption(clientProvider, httpHandlerFactory).cooccurring()
    private val verbose by option(help = "Show verbose information about nodes").flag()
    private val option by lazy {
        configuredOptions ?: manualOptions ?: throw PrintMessage("No target blockchain to analyze specified")
    }
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val networkClient = option.networkClient()
        val config = networkClient.config

        val clusterManagement = clusterManagementFactory.buildClusterManagement(networkClient)
        val nodeStatusFinder = NodeStatusFinder(httpHandlerFactory(config), clientProvider, config, clusterManagement, verbose)

        try {
            val cluster = clusterManagement.getClusterOfBlockchain(option.brid)
            val clusterUrls = clusterManagement.getBlockchainApiUrls(option.brid)

            when (outputFormat ?: if (terminal.info.outputInteractive) TableOutputFormat.table else TableOutputFormat.JSON) {
                TableOutputFormat.table -> {
                    echo(defaultTable {
                        header {
                            row("Blockchain", "Rid", "Cluster")
                        }
                        body {
                            row(option.blockchainName, option.brid.toShortHex(), clusterManagement.getClusterOfBlockchain(option.brid))
                        }
                    })
                    echo(defaultTable {
                        header {
                            row(*nodeStatusFinder.tableHeaders().toTypedArray())
                        }
                        body {
                            clusterUrls.forEach { url ->
                                val result = nodeStatusFinder.findStatus(Endpoint(url), option.brid)
                                row(*result.values())
                            }
                        }
                    })
                }

                TableOutputFormat.JSON ->
                    echo(json(mapOf(
                            "blockchain" to mapOf("Blockchain" to option.blockchainName, "Blockchain_Rid" to option.brid.toHex(), "Cluster" to cluster),
                            "nodes" to clusterUrls.map { url ->
                                val result = nodeStatusFinder.findStatus(Endpoint(url), option.brid)
                                nodeStatusFinder.tableHeaders().mapIndexed { index, header -> fixKey(header) to result.values()[index] }.toMap()
                            }
                    )))
            }
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
