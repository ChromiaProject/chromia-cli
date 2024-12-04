package com.chromia.cli.parser

import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.parser.mapByteArraysToHexString
import com.fasterxml.jackson.databind.ObjectMapper
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.yaml.BIG_INTEGER_TAG
import net.postchain.gtv.yaml.BYTE_ARRAY_TAG
import net.postchain.gtv.yaml.GtvResolver
import net.pwall.json.schema.JSONSchema
import net.pwall.json.schema.output.BasicErrorEntry
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.AbstractConstruct
import org.yaml.snakeyaml.constructor.Construct
import org.yaml.snakeyaml.env.EnvScalarConstructor
import org.yaml.snakeyaml.env.EnvScalarConstructor.ENV_FORMAT
import org.yaml.snakeyaml.env.EnvScalarConstructor.ENV_TAG
import org.yaml.snakeyaml.nodes.Node
import org.yaml.snakeyaml.nodes.ScalarNode
import org.yaml.snakeyaml.nodes.Tag
import org.yaml.snakeyaml.representer.Representer
import java.io.File

val INCLUDE_TAG = Tag("!include")

class ChromiaConstructor(val rootFile: File) : EnvScalarConstructor() {
    init {
        yamlConstructors[INCLUDE_TAG] = IncludeConstructor()
        yamlConstructors[BIG_INTEGER_TAG] = ConstructBigInteger()
        yamlConstructors[BYTE_ARRAY_TAG] = ConstructByteArray()
    }

    private inner class IncludeConstructor : Construct {
        override fun construct(p0: Node): Any {
            p0 as ScalarNode
            val rawFilePath = if (p0.value.startsWith("/")) p0.value else "${rootFile.absoluteFile.parent}/${p0.value}"
            val (path, sub) = if (rawFilePath.contains("#")) rawFilePath.split("#") else listOf(rawFilePath, "")
            return parseSubFile(File(path), sub)
        }

        private fun parseSubFile(file: File, sub: String) = file.inputStream().use {
            val yaml = createChromiaYamlParser(file)
            val result = yaml.load<Any>(it)
            if (sub.isNotBlank()) {
                require(result is Map<*, *>) { "File ${file.path} must be a dict to be able to extract a sub field" }
                require(result.containsKey(sub)) { "File ${file.path} does not contain $sub" }
                result[sub]!!
            } else {
                result
            }
        }

        override fun construct2ndStep(p0: Node?, p1: Any?) = Unit
    }

    private inner class ConstructBigInteger : AbstractConstruct() {
        override fun construct(node: Node): Any = constructScalar(node as ScalarNode).dropLast(1).toBigInteger()
    }

    private inner class ConstructByteArray : AbstractConstruct() {
        override fun construct(node: Node): Any = constructScalar(node as ScalarNode).drop(2).dropLast(1).hexStringToByteArray()
    }
}

fun createChromiaYamlParser(file: File): Yaml {
    val baseConstructor = ChromiaConstructor(file)
    val yaml = Yaml(baseConstructor, Representer(DumperOptions()), DumperOptions(), GtvResolver())
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")
    return yaml
}

fun loadAnchor(src: File, schema: JSONSchema? = null): Map<String, Any> {
    val yaml = createChromiaYamlParser(src)

    val loaded: Map<String, Any> = src.inputStream().use {
        yaml.load<Map<String, Any>>(it)
    } ?: emptyMap()

    schema?.let {
        val objectMapper = ObjectMapper()
        val formattedLoaded = mapByteArraysToHexString(loaded)
        val json = objectMapper.writeValueAsString(formattedLoaded)

        val validationResult = it.validateBasic(json)
        if (!validationResult.valid) {
            throw ValidationException(constructErrorMessage(validationResult.errors!!, src))
        }
    }

    return loaded
}

fun constructErrorMessage(errors: List<BasicErrorEntry>, src: File): String {
    val errorMessageFilterStrings = listOf("A subschema had errors", "Constant schema \"false\"", "Constant schema \"true\"")
    val filteredErrors = errors.filter { it.error !in errorMessageFilterStrings }
    return "Following errors found in ${src.name}:\n" + filteredErrors.joinToString("\n") { e -> e.error + constructLocationInfo(e) }
}

fun constructLocationInfo(e: BasicErrorEntry): String {
    return " (location: ${e.instanceLocation.removePrefix("#/").replace("/", "->")})"
}
