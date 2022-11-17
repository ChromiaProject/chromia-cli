package com.chromia.cli.parser

import com.chromia.cli.compile.CompilerOptions
import com.chromia.cli.database.DatabaseOptions
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

    fun compilerOptions(file: File): CompilerOptions {
        return mapper().readValue(file, CompilerOptions::class.java)
    }

    fun databaseOptions(file: File): DatabaseOptions {
        return mapper().readValue(file, DatabaseOptions::class.java)
    }
}