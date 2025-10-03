package com.chromia.cli.command.deployment

import com.chromia.cli.base.formatter.fixKey
import com.chromia.cli.base.formatter.json
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.NodeStatusFinder
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.TableOutputFormat
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.cm_api.cmGetBlockchainApiUrls
import com.chromia.directory1.cm_api.cmGetBlockchainCluster
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.defaultHttpHandler
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.Endpoint
import org.http4k.core.HttpHandler

class DeployInfoCommand(
        private val httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { Companion.httpHandlerFactory(it) }
) : ChromiaCommand(
        name = "info",
        help = "Information about a deployed blockchain"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config }, httpHandlerFactory)
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()
    private val verbose by option(help = "Show verbose information about nodes").flag()
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val clientConfig = settings.config.setApiUrls(target.urls)
        val directoryClient = target.createDirectoryClient(clientConfig)

        val nodeStatusFinder = NodeStatusFinder(httpHandlerFactory(directoryClient.config), PostchainClientProviderImpl(), directoryClient.config, directoryClient, verbose)

        try {
            val cluster = directoryClient.cmGetBlockchainCluster(target.brid.data)
            val clusterUrls = directoryClient.cmGetBlockchainApiUrls(target.brid)

            when (outputFormat
                    ?: if (terminal.terminalInfo.outputInteractive) TableOutputFormat.table else TableOutputFormat.JSON) {
                TableOutputFormat.table -> {
                    echo(defaultTable {
                        header {
                            row("Blockchain", "Rid", "Cluster")
                        }
                        body {
                            row(target.blockchain, target.brid.toShortHex(), cluster)
                        }
                    })
                    echo(defaultTable {
                        header {
                            row(*nodeStatusFinder.tableHeaders().toTypedArray())
                        }
                        body {
                            clusterUrls.forEach { url ->
                                val result = nodeStatusFinder.findStatus(Endpoint(url), target.brid)
                                row(*result.values())
                            }
                        }
                    })
                }

                TableOutputFormat.JSON ->
                    echo(json(mapOf(
                            "blockchain" to mapOf("Blockchain" to target.blockchain, "Blockchain_Rid" to target.brid.toHex(), "Cluster" to cluster),
                            "nodes" to clusterUrls.map { url ->
                                val result = nodeStatusFinder.findStatus(Endpoint(url), target.brid)
                                nodeStatusFinder.tableHeaders().mapIndexed { index, header -> fixKey(header) to result.values()[index] }.toMap()
                            }
                    )))
            }
        } catch (e: ClientError) {
            echo("Cluster not found for blockchain rid ${target.brid.toShortHex()}")
            echo(e.message)
        }
    }

    companion object {
        fun httpHandlerFactory(config: PostchainClientConfig) = defaultHttpHandler(config)
    }
}
