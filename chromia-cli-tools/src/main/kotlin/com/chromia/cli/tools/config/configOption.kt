package com.chromia.cli.tools.config

import com.chromia.cli.model.parseModel
import com.chromia.cli.tools.env.cliEnv
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import net.postchain.rell.api.base.RellCliEnv


// Optional Client Configuration. Reads from file system if not set
fun CliktCommand.chromiaConfigOption() = ChromiaConfigOption(cliEnv())

// Chromia model must be found or explicitly set
fun CliktCommand.chromiaModelOption() = ChromiaModelOption(cliEnv())

// Chromia model is optional. Will not throw if model file is not found
fun CliktCommand.optionalChromiaModelOption() = OptionalChromiaModelOption(cliEnv())

// Chromia model must be found or explicitly set. Client config is read from system if not set
fun CliktCommand.chromiaModelConfigOption() = ChromiaModelConfigOption(cliEnv())

// Chromia model if optional. Client config is read from system if not set
fun CliktCommand.optionalChromiaModelConfigOption() = OptionalChromiaModelConfigOption(cliEnv())

open class ChromiaConfigOption(cliEnv: RellCliEnv) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(cliEnv).loadClientConfigFile(configFile) }
}

open class ChromiaModelOption(cliEnv: RellCliEnv) : OptionGroup("Configuration Properties") {
    private val modelFile: File by requiredChromiaModelOption(cliEnv)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.sourceFile(projectFolder)
    val targetDir get() = model.compile.targetFile(projectFolder)
}

open class OptionalChromiaModelOption(cliEnv: RellCliEnv) : OptionGroup("Configuration Properties") {
    private val modelFile by chromiaModelOption()
    private val resolvedModelFile by lazy { ChromiaConfigLoader(cliEnv).findModelFile(modelFile) }
    val projectFolder by lazy { resolvedModelFile?.parentFile }
    val model by lazy { resolvedModelFile?.let { parseModel(it) } }
    val sourceDir get() = model?.compile?.sourceFile(projectFolder!!)
}


open class ChromiaModelConfigOption(cliEnv: RellCliEnv) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(cliEnv).loadClientConfigFile(configFile) }
    private val modelFile by requiredChromiaModelOption(cliEnv)
    val model by lazy { parseModel(modelFile) }
    val projectFolder by lazy { modelFile.parentFile }
    val sourceDir get() = model.compile.sourceFile(projectFolder)
    val targetDir get() = model.compile.targetFile(projectFolder)
}

open class OptionalChromiaModelConfigOption(cliEnv: RellCliEnv) : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader(cliEnv).loadClientConfigFile(configFile) }
    private val modelFile by chromiaModelOption()
    val model by lazy { ChromiaConfigLoader(cliEnv).findModelFile(modelFile)?.let { parseModel(it) } }
    val projectFolder by lazy { modelFile?.parentFile }
}

internal fun ParameterHolder.requiredChromiaModelOption(cliEnv: RellCliEnv) = chromiaModelOption()
        .defaultLazy {
            ChromiaConfigLoader(cliEnv).findModelFile(null) ?: throw PrintMessage("Project settings file not found")
        }

internal fun ParameterHolder.chromiaModelOption() = option(
        "-s", "--settings",
        help = "Alternate path for project settings file",
        metavar = "SETTINGS",
        envvar = "CHROMIA_PROJECT_SETTINGS",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }

internal fun ParameterHolder.chromiaConfigOption() = option(
        "-cfg", "--config",
        help = "Alternate path for client configuration file",
        metavar = "CONFIG",
        envvar = "CHROMIA_CONFIG",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { it.absoluteFile }
