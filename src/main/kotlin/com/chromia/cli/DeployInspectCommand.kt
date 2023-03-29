package com.chromia.cli

import com.chromia.cli.util.BlockchainAnalyzer
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.ConfiguredDeploymentInfoOption
import com.chromia.cli.util.ManualDeploymentInfoOption
import com.chromia.cli.util.modulesOption
import com.chromia.cli.util.settingsOptionDefault
import com.chromia.cli.util.settingsOptionNotRequired
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.cooccurring
import de.m3y.kformat.Table
import de.m3y.kformat.table
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.cm.cm_api.ClusterManagementImpl

class DeployInspectCommand(
        private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : CliktCommand(
        name = "inspect",
        help = "Information about any deployed blockchain"
) {

    private val settings by settingsOptionNotRequired()
    private val configuredOptions by ConfiguredDeploymentInfoOption {
        settings?.model ?: settingsOptionDefault().model
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption().cooccurring()
    private val module by modulesOption("Explicitly state which module to inspect (Comma separated)")

    override fun run() {
        val option = configuredOptions ?: manualOptions!!
        val config = PostchainClientConfig(option.brid, endpointPool = EndpointPool.default(option.urls))
        val postchainClient = clientProvider.createClient(config)

        try {
            BlockchainAnalyzer(postchainClient).getAppStructure()
                    .filter { moduleName -> module.isNullOrEmpty() || moduleName.key in module!!.map { it.str() } }
                    .forEach { (name, module) ->
                        if (!module.isEmpty()) echo("Module: $name")
                        if (!module.queries.isNullOrEmpty()) {
                            table {
                                hints {
                                    defaultAlignment = Table.Hints.Alignment.LEFT
                                    borderStyle = Table.BorderStyle.SINGLE_LINE
                                }
                                header("Query", "Return type", "parameters")
                                module.queries?.forEach { (queryName, query) ->
                                    row(queryName,
                                            query.returnType?.toString() ?: "",
                                            query.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
                                }
                            }.render().also { echo(it) }
                        }
                        if (!module.operations.isNullOrEmpty()) {
                            table {
                                hints {
                                    defaultAlignment = Table.Hints.Alignment.LEFT
                                    borderStyle = Table.BorderStyle.SINGLE_LINE
                                }
                                header("Operation", "parameters")
                                module.operations?.forEach { (operationName, operation) ->
                                    row(operationName, operation.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
                                }
                            }.render().also { echo(it) }
                        }
                        if (!module.objects.isNullOrEmpty()) {
                            table {
                                hints {
                                    defaultAlignment = Table.Hints.Alignment.LEFT
                                    borderStyle = Table.BorderStyle.SINGLE_LINE
                                }
                                header("Object", "attribute", "type", "mutable")
                                module.objects?.forEach { (objectName, objectDef) ->
                                    row(objectName)
                                    objectDef.attributes.forEach { (attribute, attributeType) ->
                                        row("", attribute, attributeType.type.toString(), attributeType.mutable.let { if (it == 1L) "Yes" else false })
                                    }
                                }
                            }.render().also { echo(it) }
                        }
                    }
        } catch (e: ClientError) {
            echo("Blockchain not found ${option.brid.toShortHex()}")
        }
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainClient) = ClusterManagementImpl(client)
    }
}
