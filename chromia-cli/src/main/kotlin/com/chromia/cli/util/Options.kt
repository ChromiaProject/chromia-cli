package com.chromia.cli.util

import com.chromia.cli.compile.NodeConfig.getNodeConfig
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.FileNotFound
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.base.model.R_ModuleName
import java.io.File


fun CliktCommand.nodePropertiesOption() =
        option("-np", "--node-properties", help = "Full path to override node properties file", metavar = "PATH")
                .file(mustExist = true, canBeDir = false, canBeFile = true)
                .convert { getNodeConfig(it) }

fun ParameterHolder.deployTargetOption() = option("--network", "-d", help = "Specify which deployment target to use")
fun ParameterHolder.blockchainOption(help: String, metavar: String? = null) =
        option("--blockchain", "-bc", help = help, metavar = metavar)

fun ParameterHolder.blockchainRidOption(help: String) = option("--blockchain-rid", "-brid", help = help)

fun CliktCommand.wipeDatabaseOption() =
        option("--wipe", help = "If a database should be wiped before startup").flag()

fun CliktCommand.showBridOption() = option(help = "Show blockchain rid").flag()

data class Settings(val file: File, val model: ChromiaModel) {
    val source get() = File(file.parentFile, model.compile.source)
    val target get() = File(file.parentFile, model.compile.target)
    val compile get() = model.compile
    val deployments get() = model.deployments
    val blockchains get() = model.blockchains
    val test get() = model.test
    val libs get() = model.libs
}

internal val DEFAULT_CONFIG_FILE = File("config.yml").absoluteFile

internal fun requireDefaultConfig() {
    if (!DEFAULT_CONFIG_FILE.exists()) {
        throw FileNotFound(DEFAULT_CONFIG_FILE.name)
    }
}

fun CliktCommand.settingsOption() = settingsOptionNotRequired()
        .defaultLazy(DEFAULT_CONFIG_FILE.name) { settingsOptionDefault() }

fun CliktCommand.settingsOptionNotRequired() =
        option("-s", "--settings", help = "Alternate path for the project settings file", metavar = "SETTINGS", envvar = "CHR_SETTINGS")
                .file(mustExist = false, canBeDir = false, canBeFile = true)
                .convert { Settings(it, parseModel(it)) }

fun settingsOptionDefault(): Settings {
    requireDefaultConfig()
    return Settings(DEFAULT_CONFIG_FILE, parseModel(DEFAULT_CONFIG_FILE))
}

fun CliktCommand.secretOption() =
        option(help = "Path to secret file (pubkey/privkey)").file(canBeDir = false, mustExist = true, canBeFile = true)

fun CliktCommand.modulesOption(help: String = "Select which modules to test, will default to tests in settings file (Comma separated)") =
        option("-m", "--modules", help = help, metavar = "MODULES")
                .split(",")

fun CliktCommand.module() = option("-m", "--module", help = "Name of module", metavar = "MODULE")
        .convert { R_ModuleName.of(it) }

fun CliktCommand.libraryOption() = option("-lib", "--library", help = "Name of library", metavar = "LIBRARY")