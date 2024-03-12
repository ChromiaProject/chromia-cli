package com.chromia.cli.parser

import com.chromia.build.tools.parser.BigIntegerDeserializer
import com.chromia.build.tools.parser.BigIntegerSerializer
import com.chromia.build.tools.parser.ByteArrayDeserializer
import com.chromia.build.tools.parser.ByteArraySerializer
import com.chromia.build.tools.parser.GtvDeserializer
import com.chromia.build.tools.parser.GtvSerializer
import com.chromia.build.tools.parser.WrappedByteArrayDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
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
import java.math.BigInteger


class ConstructorIncludeSupport(val rootFile: File) : EnvScalarConstructor() {
    init {
        yamlConstructors[Tag("!include")] = IncludeConstructor()
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
}

inline fun <reified T> GtvYaml.loadAnchor(src: File): T {
    val yaml = Yaml(ConstructorIncludeSupport(src))
    yaml.addImplicitResolver(ENV_TAG, ENV_FORMAT, "$")

    return src.inputStream().use { it ->
        ObjectMapper()
                .registerKotlinModule()
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(yaml.load(it))
                .let { MapperClass().load<T>(it) }
    }
}


class MapperClass(init: ObjectMapper.() -> Unit = {}) {
    val mapper: ObjectMapper = ObjectMapper(YAMLFactory()
            .enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR)
            .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES))
            .registerKotlinModule()
            .registerModule(SimpleModule().apply {
                addSerializer(Gtv::class.java, GtvSerializer())
                addSerializer(ByteArray::class.java, ByteArraySerializer())
                addSerializer(BigInteger::class.java, BigIntegerSerializer())
                addDeserializer(Gtv::class.java, GtvDeserializer())
                addDeserializer(ByteArray::class.java, ByteArrayDeserializer())
                addDeserializer(WrappedByteArray::class.java, WrappedByteArrayDeserializer())
                addDeserializer(BigInteger::class.java, BigIntegerDeserializer())
            })
            .also(init)

    inline fun <reified T> load(content: String): T = mapper.readValue(content, T::class.java)
}
