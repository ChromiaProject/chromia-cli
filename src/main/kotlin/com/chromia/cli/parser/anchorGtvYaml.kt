package com.chromia.cli.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.postchain.gtv.yaml.GtvYaml
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Construct
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import java.io.File


class ConstructorIncludeSupport: Constructor() {
    init {
        yamlConstructors[Tag("!include")] = IncludeConstructor()
    }

    private inner class IncludeConstructor: Construct {
        val yaml = Yaml()
        override fun construct(p0: Node): Any {
            p0 as ScalarNode
            val file = File(p0.value)
            return yaml.load(file.inputStream())
        }

        override fun construct2ndStep(p0: Node?, p1: Any?) = Unit
    }
}

inline fun <reified T> GtvYaml.loadAnchor(src: File): T {
    val yaml = Yaml(ConstructorIncludeSupport())
    return ObjectMapper()
            .registerKotlinModule()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(yaml.load(src.inputStream()))
            .let { load<T>(it) }
}
