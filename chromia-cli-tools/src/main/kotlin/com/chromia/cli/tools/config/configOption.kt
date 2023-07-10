package com.chromia.cli.tools.config

import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file


class ChromiaConfigOption : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader().loadClientConfigFile(configFile) }
}

open class ChromiaModelOption : OptionGroup("Configuration Properties") {
    val modelFile by requiredChromiaModelOption()
    val model by lazy { parseModel(modelFile) }
    val sourceDir get() = model.compile.sourceFile(modelFile.parentFile)
    val targetDir get() = model.compile.targetFile(modelFile.parentFile)
}

class OptionalChromiaModelOption : OptionGroup("Configuration Properties") {
    val modelFile by chromiaModelOption()
    val model by lazy { modelFile?.let { parseModel(it) } }
    val sourceDir get() = model?.compile?.sourceFile(modelFile!!.parentFile)
}


class ChromiaModelConfigOption : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader().loadClientConfigFile(configFile) }
    val modelFile by requiredChromiaModelOption()
    val model by lazy { parseModel(modelFile) }
    val sourceDir get() = model.compile.sourceFile(modelFile.parentFile)
    val targetDir get() = model.compile.targetFile(modelFile.parentFile)
}

class OptionalChromiaModelConfigOption : OptionGroup("Configuration Properties") {
    val configFile by chromiaConfigOption()
    val config by lazy { ChromiaConfigLoader().loadClientConfigFile(configFile) }
    val modelFile by chromiaModelOption()
    val model by lazy { modelFile?.let { parseModel(it) } }
}

internal fun ParameterHolder.requiredChromiaModelOption() = chromiaModelOption()
        .defaultLazy { ChromiaConfigLoader().findModelFile(null) }

internal fun ParameterHolder.chromiaModelOption() = option(
        "-s", "--settings",
        help = "Alternate path for project settings file (default: chromia.yml)",
        metavar = "SETTINGS",
        envvar = "CHR_SETTINGS",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { ChromiaConfigLoader().findModelFile(it) }

internal fun ParameterHolder.chromiaConfigOption() = option(
        "-cfg", "--config",
        help = "Alternate path for client configuration file",
        metavar = "CONFIG",
        envvar = "CHR_CONFIG",
)
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
