package com.chromia.cli.tools.config

import com.github.ajalt.clikt.core.ParameterHolder
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file

fun ParameterHolder.chromiaConfigOption(vararg names: String = arrayOf("-s", "--settings")) = option(*names, help = "Alternate path for project settings file", metavar = "SETTINGS", envvar = "CHR_SETTINGS")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)
        .convert { ChromiaConfig.fromModel(it) }
        .defaultLazy { ChromiaConfig.fromModel(null) }
