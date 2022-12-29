package com.chromia.cli.util

import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.parser.loadAnchor
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.yaml.GtvYaml
import net.postchain.rell.model.R_ModuleName
import java.io.File


fun CliktCommand.nodePropertiesOption() =
        option("-np", "--node-properties", help = "Full path to override node properties file", metavar = "PATH")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
                .convert { getNodeConfig(it) }

fun CliktCommand.deployTargetOption() = option("--target", help = "If a specific target deploy model should be used")

fun CliktCommand.wipeDatabaseOption() =
        option("--wipe", help = "If a database should be wiped before startup").flag()

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid").flag()

data class Settings(val file: File, val model: ChromiaCliModel) {
    val source get() = File(file.parentFile, model.compile.source)
    val target get() = File(file.parentFile, model.compile.target)
    val compile get() = model.compile
    val deployments get() = model.deployments
    val blockchains get() = model.blockchains
    val test get() = model.test
}
fun CliktCommand.settingsOption() = settingsOptionNotRequired()
        .defaultLazy("config.yml") { Settings(File("config.yml"), settingsOptionDefault()) }

fun CliktCommand.settingsOptionNotRequired() =
        option("-s", "--settings", help = "Alternate path for the project settings file", metavar = "SETTINGS")
                .file(mustExist = false, canBeDir = false, canBeFile = true)
                .convert { Settings(it, GtvYaml().loadAnchor(it)) }

fun settingsOptionDefault() = GtvYaml().loadAnchor<ChromiaCliModel>(File("config.yml"))

fun CliktCommand.secretOption() =
        option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, canBeFile = true)

fun CliktCommand.modulesOption() =
        option("-m", "--modules", help = "Select which modules to test, will default to tests in settings file (Comma separated)", metavar = "MODULES")
                .convert { R_ModuleName.of(it) }
                .split(",")

fun CliktCommand.module() = option("-m", "--module", help = "Name of module", metavar = "MODULE")
        .convert { R_ModuleName.of(it) }