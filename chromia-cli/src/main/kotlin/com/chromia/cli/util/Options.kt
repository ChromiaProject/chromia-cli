package com.chromia.cli.util

import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.base.model.R_ModuleName

@Suppress("EnumEntryName")
enum class OutputFormat {
    pretty, raw, JSON, XML
}

@Suppress("EnumEntryName")
enum class TableOutputFormat {
    table, JSON
}

fun CliktCommand.outputFormat() = option("-f", "--output-format", help = "Output format").enum<OutputFormat>()
        .default(OutputFormat.pretty)

fun CliktCommand.tableOutputFormat() = option("-f", "--output-format", help = "Output format").enum<TableOutputFormat>()

fun CliktCommand.nodePropertiesOption() =
        option("-np", "--node-properties", help = "Full path to override node properties file", metavar = "PATH")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
                .convert { getNodeConfig(it) }

fun ParameterHolder.deployTargetOption() = option("--network", "-d", help = "Specify which deployment target to use")
fun ParameterHolder.blockchainOption(help: String, metavar: String? = null) =
        option("--blockchain", "-bc", help = help, metavar = metavar)

fun CliktCommand.wipeDatabaseOption() =
        option("--wipe", help = "If a database should be wiped before startup").flag("--no-wipe")

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid").flag()

fun ParameterHolder.publicKeyOption(help: String = "Set public key explicitly") = option("-pk", "--pubkey", help = help)
        .validate {
            require(it.matches(Regex("[0-9A-Fa-f]+"))) { "Public key contains one ore more illegal character. Supported Characters are: 0-9, A-F, a-f." }
            require(it.length % 2 == 0) { "The public key must be a hex string with even length. Length was: ${it.length}" }
        }

fun CliktCommand.secretOption() =
        option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, mustBeReadable = true)

fun CliktCommand.modulesOption(help: String) =
        option("-m", "--modules", help = help, metavar = "MODULES")
                .split(",")

fun CliktCommand.module() = option("-m", "--module", help = "Name of module", metavar = "MODULE")
        .convert { R_ModuleName.of(it) }

fun CliktCommand.libraryOption() = option("-lib", "--library", help = "Name of library", metavar = "LIBRARY")
fun ParameterHolder.logSqlOption() = option("--sql-log", help = "Log sql expressions").flag()

fun ParameterHolder.containerIdOption(help: String = "Set container id explicitly") = option("-cid", "--container-id", help = help)

fun ParameterHolder.targetDirectoryOption(help: String) = option("-d", "--target", help = help)
        .file(canBeFile = false)