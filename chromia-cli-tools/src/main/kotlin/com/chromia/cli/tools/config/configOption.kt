package com.chromia.cli.tools.config

import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.BlockchainRid
import java.io.File


// Optional Client Configuration. Reads from file system if not set
fun CliktCommand.chromiaConfigOption() = ChromiaConfigOption(::echo)

// Chromia model must be found or explicitly set
fun CliktCommand.chromiaModelOption() = ChromiaModelOption(::echo)

// Chromia model is optional. Will not throw if model file is not found
fun CliktCommand.optionalChromiaModelOption() = OptionalChromiaModelOption(::echo)

// Chromia model must be found or explicitly set. Client config is read from system if not set
fun CliktCommand.chromiaModelConfigOption() = ChromiaModelConfigOption(::echo)

// Chromia model if optional. Client config is read from system if not set
fun CliktCommand.optionalChromiaModelConfigOption() = OptionalChromiaModelConfigOption(::echo)

open class ChromiaConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
}

open class ChromiaModelOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    private val modelFile: File by requiredChromiaModelOption(logger)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.source.toFile()
    val targetDir get() = model.compile.target.toFile()
}

open class OptionalChromiaModelOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    private val modelFile by chromiaModelOption()
    private val resolvedModelFile by lazy { ChromiaConfigLoader(logger).findModelFile(modelFile) }
    val projectFolder by lazy { resolvedModelFile?.parentFile }
    val model by lazy { resolvedModelFile?.let { parseModel(it) } }
    val sourceDir get() = model?.compile?.source?.toFile()
    val targetDir get() = model?.compile?.target?.toFile()
}


open class ChromiaModelConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
    val modelFile by requiredChromiaModelOption(logger)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.source.toFile()
    val targetDir get() = model.compile.target.toFile()
}

open class OptionalChromiaModelConfigOption(logger: (String) -> Unit) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(configFile) }
    private val modelFile by chromiaModelOption()
    val model by lazy { ChromiaConfigLoader(logger).findModelFile(modelFile)?.let { parseModel(it) } }
    val projectFolder by lazy { modelFile?.parentFile }
}

open class BlockchainOptions(logger: (String) -> Unit) : OptionGroup() {
    private val clientConfigFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(logger).loadClientConfigFile(clientConfigFile) }
    val url by targetUrlOption()
    val blockchainRid by blockchainRidOption("Target Blockchain RID").convert { BlockchainRid.buildFromHex(it) }.required()
}

internal fun ParameterHolder.requiredChromiaModelOption(logger: (String) -> Unit) = chromiaModelOption()
        .defaultLazy {
            ChromiaConfigLoader(logger).findModelFile(null)
                    ?: throw PrintMessage("Project settings file not found", statusCode = 1)
        }

internal fun ParameterHolder.chromiaModelOption() = option(
        "-s", "--settings",
        help = "Alternate path for project settings file",
        metavar = "SETTINGS",
        envvar = "CHROMIA_PROJECT_SETTINGS",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }

fun ParameterHolder.chromiaConfigOption() = option(
        "-cfg", "--config",
        help = "Alternate path for client configuration file",
        metavar = "CONFIG",
        envvar = "CHROMIA_CONFIG",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }

fun ParameterHolder.targetUrlOption() = option("--api-url", help = "Target url")

fun ParameterHolder.blockchainRidOption(help: String) = option("--blockchain-rid", "-brid", help = help)

enum class ConfigurationFormat {
    GTV, XML
}

fun ParameterHolder.configurationFormatOption() = option("-f", "--format", help = "Blockchain configuration format")
        .enum<ConfigurationFormat>().default(ConfigurationFormat.XML)
