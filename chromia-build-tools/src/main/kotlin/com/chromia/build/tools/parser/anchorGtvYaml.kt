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


class ConstructorIncludeSupport(val rootFile: File): EnvScalarConstructor() {
    init {
        yamlConstructors[Tag("!include")] = IncludeConstructor()
    }

    private inner class IncludeConstructor: Construct {
        val yaml = Yaml()
        override fun construct(p0: Node): Any {
            p0 as ScalarNode
            val rawFilePath = if (p0.value.startsWith("/")) p0.value else "${rootFile.absoluteFile.parent}/${p0.value}"
            val (path, sub) = if (rawFilePath.contains("#")) rawFilePath.split("#") else listOf(rawFilePath, "")
            return parseSubFile(File(path), sub)
        }

        fun parseSubFile(file: File, sub: String) = file.inputStream().use {
            val result = yaml.load<Any>(it)
            if (sub.isNotBlank()) {
                require(result is Map<*,*>) { "File ${file.path} must be a dict to be able to extract a sub field"}
                require(result.containsKey(sub)) { "File ${file.path} does not contain $sub" }
                result[sub]!!
            } else {
                result
            }
        }

        override fun construct2ndStep(p0: Node?, p1: Any?) = Unit
    }
}

inline fun <reified T> GtvYaml.loadAnchor(src: File): T {
    val yaml = Yaml(ConstructorIncludeSupport(src))
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")

    return src.inputStream().use {
        ObjectMapper()
                .registerKotlinModule()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(yaml.load(it))
                .let { load<T>(it) }
    }
}
