package com.chromia.cli.parser

import com.chromia.build.tools.compile.ValidationException
import com.chromia.build.tools.parser.TestAnchor
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.yaml.BIG_INTEGER_FORMAT
import net.postchain.gtv.yaml.BIG_INTEGER_START
import net.postchain.gtv.yaml.BIG_INTEGER_TAG
import net.postchain.gtv.yaml.BYTE_ARRAY_FORMAT
import net.postchain.gtv.yaml.BYTE_ARRAY_START
import net.postchain.gtv.yaml.BYTE_ARRAY_TAG
import net.postchain.gtv.yaml.GtvRepresenter
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
import org.yaml.snakeyaml.serializer.NumberAnchorGenerator
import java.io.File

val INCLUDE_TAG = Tag("!include")

class ChromiaConstructor(val rootFile: File) : EnvScalarConstructor() {
    init {
        yamlConstructors[INCLUDE_TAG] = IncludeConstructor()
        yamlConstructors[BIG_INTEGER_TAG] = ConstructBigInteger()
        yamlConstructors[BYTE_ARRAY_TAG] = ConstructByteArray()
    }

    private inner class IncludeConstructor : Construct {
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

fun loadAnchor(src: File, schema: JSONSchema? = null): Map<String, Any> {
    val baseConstructor = ChromiaConstructor(src)
    val yaml = Yaml(baseConstructor)
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")
    yaml.addImplicitResolver(BIG_INTEGER_TAG, BIG_INTEGER_FORMAT, BIG_INTEGER_START)
    yaml.addImplicitResolver(BYTE_ARRAY_TAG, BYTE_ARRAY_FORMAT, BYTE_ARRAY_START)

    val loaded = src.inputStream().use {
        yaml.load<Map<String, Any>>(it)
    }
    println(loaded)
    schema?.let {
        val jsonDumper = Yaml(GtvRepresenter(), DumperOptions().apply {
            defaultFlowStyle = DumperOptions.FlowStyle.FLOW
            defaultScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED
            width = Int.MAX_VALUE
            //anchorGenerator = TestAnchor() //this is where the indexes is set
        })

        val json = jsonDumper.dump(loaded)
                .replace("""!!null "null"""", "null")
                .replace("""!!bool "true"""", "true")
                .replace("""!!bool "false"""", "false")
                .replace("""!!int "([+-]?[0-9]+)"""".toRegex(), "$1")
                .replace("""!biginteger """, "")
                .replace("""!bytearray """, "")

        println(json)
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
