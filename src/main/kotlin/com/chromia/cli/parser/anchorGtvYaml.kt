package com.chromia.cli.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.postchain.gtv.yaml.GtvYaml
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Construct
import org.yaml.snakeyaml.env.EnvScalarConstructor
import org.yaml.snakeyaml.env.EnvScalarConstructor.ENV_FORMAT
import org.yaml.snakeyaml.env.EnvScalarConstructor.ENV_TAG
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import java.io.File


class ConstructorIncludeSupport: EnvScalarConstructor() {
    init {
        yamlConstructors[Tag("!include")] = IncludeConstructor()
    }

    private inner class IncludeConstructor: Construct {
        val yaml = Yaml()
        override fun construct(p0: Node): Any {
            p0 as ScalarNode
            return if (p0.value.contains("#")) {
                val (f, sub) = p0.value.split("#")
                val result = yaml.load<Map<String, Any>>(File(f).inputStream())
                require(result.containsKey(sub)) { "File $f does not contain $sub" }
                result[sub]!!
            } else {
                val file = File(p0.value)
                yaml.load(file.inputStream())
            }
        }

        override fun construct2ndStep(p0: Node?, p1: Any?) = Unit
    }
}

inline fun <reified T> GtvYaml.loadAnchor(src: File): T {
    val yaml = Yaml(ConstructorIncludeSupport())
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")
    return ObjectMapper()
            .registerKotlinModule()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(yaml.load(src.inputStream()))
            .let { load<T>(it) }
}
