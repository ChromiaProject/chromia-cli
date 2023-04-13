package com.chromia.cli

import com.chromia.cli.util.BlockchainAnalyzer
import com.chromia.cli.util.ConfiguredDeploymentInfoOption
import com.chromia.cli.util.ManualDeploymentInfoOption
import com.chromia.cli.util.RellFunction
import com.chromia.cli.util.RellObject
import com.chromia.cli.util.modulesOption
import com.chromia.cli.util.settingsOptionDefault
import com.chromia.cli.util.settingsOptionNotRequired
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.cooccurring
import de.m3y.kformat.Table
import de.m3y.kformat.table
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool

class DeployInspectCommand(
        private val clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
) : CliktCommand(
        name = "inspect",
        help = "Inspect the API of a deployed blockchain"
) {

    private val settings by settingsOptionNotRequired()
    private val configuredOptions by ConfiguredDeploymentInfoOption {
        settings?.model ?: settingsOptionDefault().model
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption().cooccurring()
    private val moduleOption by modulesOption("Explicitly state which module to inspect (Comma separated)")
    private val option by lazy { configuredOptions ?: manualOptions ?: throw PrintMessage("No target blockchain to analyze specified") }

    override fun run() {
        val config = PostchainClientConfig(option.brid, endpointPool = EndpointPool.default(option.urls))
        val postchainClient = clientProvider.createClient(config)

        try {
            BlockchainAnalyzer(postchainClient).getAppStructure()
                    .filter { moduleName -> moduleOption.isNullOrEmpty() || moduleName.key in moduleOption!! }
                    .filterValues { !it.isEmpty() }
                    .forEach { (name, module) ->
                        echo("Module: $name")
                        if (!module.queries.isNullOrEmpty()) {
                            tableOfQueries(module.queries)
                        }
                        if (!module.operations.isNullOrEmpty()) {
                            tableOfOperations(module.operations)
                        }
                        if (!module.objects.isNullOrEmpty()) {
                            tableOfObjects(module.objects)
                        }
                    }
        } catch (e: ClientError) {
            echo("Blockchain not found ${option.brid.toShortHex()}")
        }
    }

    private fun tableOfQueries(queries: Map<String, RellFunction>) {
        table {
            hints {
                defaultAlignment = Table.Hints.Alignment.LEFT
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
            header("Query", "Return type", "Parameters")
            queries.forEach { (queryName, query) ->
                row(queryName,
                        query.returnType?.toString() ?: "",
                        query.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
            }
        }.render().also { echo(it) }
    }

    private fun tableOfOperations(operations: Map<String, RellFunction>) {
        table {
            hints {
                defaultAlignment = Table.Hints.Alignment.LEFT
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
            header("Operation", "Parameters")
            operations.forEach { (operationName, operation) ->
                row(operationName, operation.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
            }
        }.render().also { echo(it) }
    }

    private fun tableOfObjects(objects: Map<String, RellObject>) {
        table {
            hints {
                defaultAlignment = Table.Hints.Alignment.LEFT
                borderStyle = Table.BorderStyle.SINGLE_LINE
            }
            header("Object", "Attribute", "Type", "Mutable")
            objects.forEach { (objectName, objectDef) ->
                row(objectName)
                objectDef.attributes.forEach { (attribute, attributeType) ->
                    row("", attribute, attributeType.type.toString(), attributeType.mutable.let { if (it == 1L) "Yes" else false })
                }
            }
        }.render().also { echo(it) }
    }
}
