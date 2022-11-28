package com.chromia.cli.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.postchain.gtv.yaml.GtvYaml
import org.yaml.snakeyaml.Yaml
import java.io.File


inline fun <reified T> GtvYaml.loadAnchor(src: File): T {
    return ObjectMapper()
            .registerKotlinModule()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(Yaml().load(src.inputStream()))
            .let { load<T>(it) }
}
