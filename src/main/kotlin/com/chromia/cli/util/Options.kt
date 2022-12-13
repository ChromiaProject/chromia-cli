package com.chromia.cli.util

import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.parser.loadAnchor
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.path
import net.postchain.common.BlockchainRid
import net.postchain.gtv.yaml.GtvYaml
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.runtime.Rt_ChainSqlMapping
import java.io.File
import java.nio.file.Path


fun CliktCommand.nodePropertiesOption() =
        option("-np", "--node-properties", help = "Full path to override node properties file", metavar = "PATH")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
                .convert { getNodeConfig(it) }

fun CliktCommand.deployTargetOption() = option("--target", help = "If a specific target deploy model should be used")

fun CliktCommand.wipeDatabaseOption() =
        option("--wipe", help = "If a database should be wiped before startup").flag()

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid").flag()

fun CliktCommand.settingsOption() = option("-s", "--settings", help = "Alternate path for the project settings file", metavar = "SETTINGS")
        .file(mustExist = true, canBeDir = false, canBeFile = true)
        .convert { GtvYaml().loadAnchor<ChromiaCliModel>(it) }

        .defaultLazy("config.yml") {
            try {
                GtvYaml().loadAnchor(File("config.yml"))
            } catch (e: Exception) {
                ChromiaCliModel()
            }
        }

fun CliktCommand.secretOption() =
        option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, canBeFile = true)

fun CliktCommand.modulesOption() =
        option("-m", "--modules", help = "Select which modules to test, will default to tests in settings file (Comma separated)", metavar = "MODULES")
                .convert { R_ModuleName.of(it) }
                .split(",")

fun CliktCommand.module() = option("-m", "--module", help = "Name of module", metavar = "MODULE")
        .convert { R_ModuleName.of(it) }

fun CliktCommand.entry() = option("-e", "--entry", help = "Name of method", metavar = "METHOD")

fun CliktCommand.arguments() = option("-a", "--args", help = "Single or multiple arguments, (-a foo -a bar)", metavar = "GTV ARGUMENTS")
        .multiple(listOf())