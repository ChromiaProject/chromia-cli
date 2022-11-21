package com.chromia.cli.parser

import com.chromia.cli.config.CompilerConfig
import com.chromia.cli.config.DatabaseConfig
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class YamlParser {
    private fun mapper(): ObjectMapper {
        return ObjectMapper(YAMLFactory())
                .registerKotlinModule()
                .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
    }

    fun compilerOptions(file: File): CompilerConfig {
        return mapper().readValue(file, CompilerConfig::class.java)
    }

    fun databaseOptions(file: File): DatabaseConfig {
        return mapper().readValue(file, DatabaseConfig::class.java)
    }
}