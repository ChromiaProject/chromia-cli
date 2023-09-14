package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.BlockchainAnalyzer
import com.chromia.cli.util.ConfiguredDeploymentInfoOption
import com.chromia.cli.util.ManualDeploymentInfoOption
import com.chromia.cli.util.RellFunction
import com.chromia.cli.util.RellObject
import com.chromia.cli.util.modulesOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.defaultHttpHandler
import net.postchain.client.exception.ClientError
import net.postchain.client.impl.PostchainClientProviderImpl
import org.http4k.core.HttpHandler

class DeployInspectCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        httpHandlerFactory: (PostchainClientConfig) -> HttpHandler = { defaultHttpHandler(it) }
) : CliktCommand(
        name = "inspect",
        help = "Inspect the API of a deployed blockchain"
) {

    private val settings by optionalChromiaModelOption()
    private val configuredOptions by ConfiguredDeploymentInfoOption(clientProvider) {
        settings.model ?: ChromiaModel()
    }.cooccurring()
    private val manualOptions by ManualDeploymentInfoOption(clientProvider, httpHandlerFactory).cooccurring()
    private val moduleOption by modulesOption("Explicitly state which module to inspect (Comma separated)")
    private val option by lazy {
        configuredOptions ?: manualOptions ?: throw PrintMessage("No target blockchain to analyze specified")
    }
    private val moduleNamesOnly by option("-l", "--list-modules", help = "List all module names").flag()

    override fun run() {
        try {
            val appStructure = BlockchainAnalyzer(option.blockchainClient()).getAppStructure()
            if (moduleNamesOnly) {
                echo(defaultTable {
                    header { row("Module") }
                    body {
                        appStructure.keys.forEach { row(it) }
                    }
                })
            } else {
                appStructure.filter { moduleName -> moduleOption.isNullOrEmpty() || moduleName.key in moduleOption!! }
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
            }
        } catch (e: ClientError) {
            echo("Blockchain not found ${option.brid.toShortHex()}")
        }
    }

    private fun tableOfQueries(queries: Map<String, RellFunction>) {
        echo(defaultTable {
            header { row("Query", "Return type", "Parameters") }
            body {
                queries.forEach { (queryName, query) ->
                    row(queryName,
                            query.returnType?.toString() ?: "",
                            query.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
                }
            }
        })
    }

    private fun tableOfOperations(operations: Map<String, RellFunction>) {
        echo(defaultTable {
            header { row("Operation", "Parameters") }
            body {
                operations.forEach { (operationName, operation) ->
                    row(operationName, operation.parameters.joinToString(", ") { "${it.name}: ${it.type}" })
                }
            }
        })
    }

    private fun tableOfObjects(objects: Map<String, RellObject>) {
        echo(defaultTable {
            header { row("Object", "Attribute", "Type", "Mutable") }
            body {
                objects.forEach { (objectName, objectDef) ->
                    row(objectName)
                    objectDef.attributes.forEach { (attribute, attributeType) ->
                        row("", attribute, attributeType.type.toString(), attributeType.mutable.let { if (it == 1L) "Yes" else "No" })
                    }
                }
            }
        })
    }
}
