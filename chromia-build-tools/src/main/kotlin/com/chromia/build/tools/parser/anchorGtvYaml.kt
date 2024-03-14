package com.chromia.cli.parser

import com.chromia.build.tools.compile.ValidationException
import net.jimblackler.jsonschemafriend.Schema
import net.jimblackler.jsonschemafriend.ValidationError
import net.jimblackler.jsonschemafriend.Validator
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.yaml.BIG_INTEGER_TAG
import net.postchain.gtv.yaml.BYTE_ARRAY_TAG
import net.postchain.gtv.yaml.GtvResolver
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
        val yaml = Yaml()
        override fun construct(p0: Node): Any {
            p0 as ScalarNode
            val rawFilePath = if (p0.value.startsWith("/")) p0.value else "${rootFile.absoluteFile.parent}/${p0.value}"
            val (path, sub) = if (rawFilePath.contains("#")) rawFilePath.split("#") else listOf(rawFilePath, "")
            return parseSubFile(File(path), sub)
        }

        fun parseSubFile(file: File, sub: String): Any = file.inputStream().use {
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

fun loadAnchor(src: File, schema: Schema? = null): Map<String, Any> {
    val yaml = Yaml(ChromiaConstructor(src), Representer(DumperOptions()), DumperOptions(), GtvResolver())
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")

    val loaded = src.inputStream().use {
        yaml.load<Map<String, Any>>(it)
    }

    schema?.let {
        val validator = Validator()
        validator.validate(it, loaded) { error ->
            throw ValidationException(constructErrorMessage(error, src))
        }
    }

    return loaded
}

fun constructErrorMessage(error: ValidationError, src: File): String =
        "Following errors found in ${src.name}:\n" + error.toString()
