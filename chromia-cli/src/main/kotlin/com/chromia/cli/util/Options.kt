package com.chromia.cli.util

import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.base.model.R_ModuleName


fun CliktCommand.nodePropertiesOption() =
        option("-np", "--node-properties", help = "Full path to override node properties file", metavar = "PATH")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
                .convert { getNodeConfig(it) }

fun ParameterHolder.deployTargetOption() = option("--network", "-d", help = "Specify which deployment target to use")
fun ParameterHolder.blockchainOption(help: String, metavar: String? = null) =
        option("--blockchain", "-bc", help = help, metavar = metavar)

fun ParameterHolder.blockchainRidOption(help: String) = option("--blockchain-rid", "-brid", help = help)

fun CliktCommand.wipeDatabaseOption() =
        option("--wipe", help = "If a database should be wiped before startup").flag("--no-wipe")

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid").flag()

fun CliktCommand.secretOption() =
        option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, mustBeReadable = true)

fun CliktCommand.modulesOption(help: String = "Select which modules to test, will default to tests in settings file (Comma separated)") =
        option("-m", "--modules", help = help, metavar = "MODULES")
                .split(",")

fun CliktCommand.module() = option("-m", "--module", help = "Name of module", metavar = "MODULE")
        .convert { R_ModuleName.of(it) }

fun CliktCommand.libraryOption() = option("-lib", "--library", help = "Name of library", metavar = "LIBRARY")
fun ParameterHolder.logSqlOption() = option("--sql-log", help = "Log sql expressions").flag()
