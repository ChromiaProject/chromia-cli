package com.chromia.cli.command.deployment

import com.chromia.cli.base.formatter.json
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.tools.formatter.header
import com.chromia.cli.util.BlockchainAnalyzer
import com.chromia.cli.util.ExplicitDeploymentOption
import com.chromia.cli.util.ModuleArgsAnalyzer
import com.chromia.cli.util.RellBlockchainAnalyzer
import com.chromia.cli.util.RellEntity
import com.chromia.cli.util.RellModuleArgsAnalyzer
import com.chromia.cli.util.RellObject
import com.chromia.cli.util.RellOperation
import com.chromia.cli.util.RellQuery
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.TableOutputFormat
import com.chromia.cli.util.modulesOption
import com.chromia.cli.util.tableOutputFormat
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.table.Borders
import com.github.ajalt.mordant.table.ColumnWidth
import net.postchain.client.core.PostchainClient
import net.postchain.client.exception.ClientError
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvString
import net.postchain.gtv.pretty

class DeployInspectCommand(
        private val blockchainAnalyzerFactory: (PostchainClient) -> BlockchainAnalyzer = { RellBlockchainAnalyzer(it) },
        private val moduleArgsAnalyzerFactory: (PostchainClient) -> ModuleArgsAnalyzer = { RellModuleArgsAnalyzer(it) },
) : ChromiaCommand(
        name = "inspect",
        help = "Inspect the API of a deployed blockchain"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by ExplicitDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    private val outputFormat by tableOutputFormat().defaultLazy {
        if (terminal.terminalInfo.outputInteractive) TableOutputFormat.table else TableOutputFormat.JSON
    }
    private val moduleOption by modulesOption("Explicitly state which module to inspect (comma separated)")

    sealed class Filter {
        data object ModuleNamesOnly : Filter()
        data object ModuleArgs : Filter()
        data class Definitions(val kinds: Set<RellKind>) : Filter()
        data class Signature(val signature: String) : Filter()
        data object All : Filter()
    }

    private val filter: Filter by mutuallyExclusiveOptions(
            option("-l", "--list-modules", help = "List all module names").flag().convert { Filter.ModuleNamesOnly },
            option("--module-args", help = "Show module_args").flag().convert { Filter.ModuleArgs },
            option("--definitions", help = "List definitions of this kind (comma separated), default: all")
                    .choice(*RellKind.entries.map { it.toString() }.toTypedArray())
                    .convert {
                        Filter.Definitions(it.split(',').map(RellKind::valueOf).toSet())
                    },
            option("--signature", help = "Show the signature of the specified definition and exit").convert { Filter.Signature(it) },
    ).single().default(Filter.All)

    private val typeStyle = TextColors.rgb("#97C2B9")
    private val mutableStyle = TextColors.rgb("#569CD6")

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val clientConfig = settings.config.setApiUrls(target.urls).setBrid(target.brid)
        val client = target.createClient(clientConfig)

        val definitionsFilter = (filter as? Filter.Definitions)?.kinds ?: RellKind.entries.toSet()
        try {
            val appStructure = blockchainAnalyzerFactory(client).getAppStructure()
            when (outputFormat) {
                TableOutputFormat.table ->
                    when (filter) {
                        is Filter.ModuleNamesOnly -> {
                            echo(defaultTable {
                                captionTop(terminal.theme.header("Modules"))
                                tableBorders = Borders.ALL
                                cellBorders = Borders.LEFT_RIGHT
                                body {
                                    appStructure.keys.forEach { row(it) }
                                }
                            })
                        }

                        is Filter.ModuleArgs -> {
                            val moduleArgsValues = moduleArgsAnalyzerFactory(client).getModuleArgs()
                            appStructure.filterKeys { moduleFilter(it) }.forEach { (moduleName, module) ->
                                module.structures?.entries?.firstOrNull { it.key == "module_args" }?.value?.let {
                                    echo(moduleArgsValues?.get(moduleName)?.let { argsValues ->
                                        defaultTable {
                                            captionTop(terminal.theme.header(moduleName))
                                            column(0) {
                                                width = ColumnWidth.Auto
                                            }
                                            column(1) {
                                                width = ColumnWidth.Auto
                                            }
                                            column(2) {
                                                width = ColumnWidth.Expand()
                                            }
                                            header { row("module_arg", "Type", "Value") }
                                            body {
                                                it.attributes.forEach { (name, attributeType) ->
                                                    row(name, typeStyle(formatTypeTable(attributeType)), argsValues[name]?.pretty()
                                                            ?: "")
                                                }
                                            }
                                        }
                                    } ?: defaultTable {
                                        captionTop(terminal.theme.header(moduleName))
                                        header { row("module_arg", "Type") }
                                        body {
                                            it.attributes.forEach { (name, attributeType) ->
                                                row(name, typeStyle(formatTypeTable(attributeType)))
                                            }
                                        }
                                    })
                                }
                            }
                        }

                        is Filter.Signature -> {
                            val signature = (filter as Filter.Signature).signature
                            tableOfQueries(appStructure.values.mapNotNull { it.queries?.values?.firstOrNull { v -> v.mount == signature } })
                            tableOfOperations(appStructure.values.mapNotNull { it.operations?.values?.firstOrNull { v -> v.mount == signature } })
                            tableOfEntities(appStructure.values.mapNotNull { it.entities?.values?.firstOrNull { v -> v.mount == signature } })
                            tableOfObjects(appStructure.values.mapNotNull { it.objects?.values?.firstOrNull { v -> v.mount == signature } })
                        }

                        else -> {
                            if (RellKind.queries in definitionsFilter)
                                tableOfQueries(appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                    it.queries?.values ?: listOf()
                                })
                            if (RellKind.operations in definitionsFilter)
                                tableOfOperations(appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                    it.operations?.values ?: listOf()
                                })
                            if (RellKind.entities in definitionsFilter)
                                tableOfEntities(appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                    it.entities?.values ?: listOf()
                                })
                            if (RellKind.objects in definitionsFilter)
                                tableOfObjects(appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                    it.objects?.values ?: listOf()
                                })
                        }
                    }

                TableOutputFormat.JSON ->
                    when (filter) {
                        is Filter.ModuleNamesOnly ->
                            echo(json(appStructure.keys))

                        is Filter.ModuleArgs -> {
                            val moduleArgsValues = moduleArgsAnalyzerFactory(client).getModuleArgs()
                            echo(json(appStructure.filterKeys { moduleFilter(it) }.map { (moduleName, module) ->
                                module.structures?.entries?.firstOrNull { it.key == "module_args" }?.value?.let {
                                    moduleArgsValues?.get(moduleName)?.let { argsValues ->
                                        mapOf(
                                                "module_name" to moduleName,
                                                "module_args" to it.attributes.map { (name, attributeType) ->
                                                    mapOf("name" to name, "type" to formatTypeJson(attributeType), "value" to argsValues[name]?.toString())
                                                }
                                        )
                                    } ?: mapOf(
                                            "module_name" to moduleName,
                                            "module_args" to it.attributes.map { (name, attributeType) ->
                                                mapOf("name" to name, "type" to formatTypeJson(attributeType))
                                            }
                                    )
                                }
                            }.filterNotNull()))
                        }

                        is Filter.Signature -> {
                            val signature = (filter as Filter.Signature).signature

                            echo(json(appStructure.values.firstNotNullOfOrNull { it.queries?.values?.firstOrNull { v -> v.mount == signature } }?.let { jsonOfQuery(it) }
                                    ?: appStructure.values.firstNotNullOfOrNull { it.operations?.values?.firstOrNull { v -> v.mount == signature } }?.let { jsonOfOperation(it) }
                                    ?: appStructure.values.firstNotNullOfOrNull { it.entities?.values?.firstOrNull { v -> v.mount == signature } }?.let { jsonOfEntity(it) }
                                    ?: appStructure.values.firstNotNullOfOrNull { it.objects?.values?.firstOrNull { v -> v.mount == signature } }?.let { jsonOfObject(it) }
                                    ?: mapOf<String, Any>()))
                        }

                        else -> {
                            echo(json(buildMap {
                                if (RellKind.queries in definitionsFilter)
                                    put("queries", appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                        it.queries?.values ?: listOf()
                                    }.map { jsonOfQuery(it) })
                                if (RellKind.operations in definitionsFilter)
                                    put("operations", appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                        it.operations?.values ?: listOf()
                                    }.map { jsonOfOperation(it) })
                                if (RellKind.entities in definitionsFilter)
                                    put("entities", appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                        it.entities?.values ?: listOf()
                                    }.map { jsonOfEntity(it) })
                                if (RellKind.objects in definitionsFilter)
                                    put("objects", appStructure.filterKeys { moduleFilter(it) }.values.flatMap {
                                        it.objects?.values ?: listOf()
                                    }.map { jsonOfObject(it) })
                            }))
                        }
                    }
            }
        } catch (e: ClientError) {
            echo("Blockchain not found ${client.config.blockchainRid.toShortHex()}: ${e.message}")
        }
    }

    private fun moduleFilter(moduleName: String) = moduleOption.isNullOrEmpty() || moduleName in moduleOption!!

    private fun tableOfQueries(queries: Collection<RellQuery>) {
        if (queries.isNotEmpty()) {
            echo(defaultTable {
                column(0) {
                    width = ColumnWidth.Auto
                }
                column(1) {
                    width = ColumnWidth.Auto
                }
                column(2) {
                    width = ColumnWidth.Expand()
                }
                header { row("Query", "Return type", "Parameters") }
                body {
                    queries.forEach { query ->
                        row(
                                query.mount,
                                typeStyle(formatTypeTable(query.returnType)),
                                query.parameters.joinToString("\n") { "${it.name}: ${typeStyle(formatTypeTable(it.type))}" })
                    }
                }
            })
        }
    }

    private fun jsonOfQuery(query: RellQuery) =
            mapOf(
                    "mount_name" to query.mount,
                    "return_type" to formatTypeJson(query.returnType),
                    "parameters" to query.parameters.associate { it.name to formatTypeJson(it.type) }
            )

    private fun tableOfOperations(operations: Collection<RellOperation>) {
        if (operations.isNotEmpty()) {
            echo(defaultTable {
                column(0) {
                    width = ColumnWidth.Auto
                }
                column(1) {
                    width = ColumnWidth.Expand()
                }
                header { row("Operation", "Parameters") }
                body {
                    operations.forEach { operation ->
                        row(operation.mount, operation.parameters.joinToString("\n") { "${it.name}: ${typeStyle(formatTypeTable(it.type))}" })
                    }
                }
            })
        }
    }

    private fun jsonOfOperation(operation: RellOperation) =
            mapOf("mount_name" to operation.mount, "parameters" to operation.parameters.map {
                mapOf("name" to it.name, "type" to formatTypeJson(it.type))
            })

    private fun tableOfEntities(entities: Collection<RellEntity>) {
        if (entities.isNotEmpty()) {
            echo(defaultTable {
                column(0) {
                    width = ColumnWidth.Auto
                }
                column(1) {
                    width = ColumnWidth.Expand()
                }
                header { row("Entity", "Attributes") }
                body {
                    entities.forEach { entity ->
                        row(entity.mount, entity.attributes.joinToString("\n") {
                            "${if (it.mutable) mutableStyle("mutable ") else ""}${it.name}: ${typeStyle(formatTypeTable(it.type))}"
                        })
                    }
                }
            })
        }
    }

    private fun jsonOfEntity(entity: RellEntity) =
            mapOf("mount_name" to entity.mount, "attributes" to entity.attributes.map {
                mapOf("name" to it.name, "type" to formatTypeJson(it.type), "mutable" to it.mutable)
            })

    private fun tableOfObjects(objects: Collection<RellObject>) {
        if (objects.isNotEmpty()) {
            echo(defaultTable {
                column(0) {
                    width = ColumnWidth.Auto
                }
                column(1) {
                    width = ColumnWidth.Expand()
                }
                header { row("Object", "Attributes") }
                body {
                    objects.forEach { obj ->
                        row(obj.mount, obj.attributes.joinToString("\n") {
                            "${if (it.mutable) mutableStyle("mutable ") else ""}${it.name}: ${typeStyle(formatTypeTable(it.type))}"
                        })
                    }
                }
            })
        }
    }

    private fun jsonOfObject(obj: RellObject) =
            mapOf("mount_name" to obj.mount, "attributes" to obj.attributes.map {
                mapOf("name" to it.name, "type" to formatTypeJson(it.type), "mutable" to it.mutable)
            })

    private fun formatTypeTable(type: Gtv) = if (type is GtvString) type.asString() else "<complex type>"

    private fun formatTypeJson(type: Gtv) = type.toString()
}
