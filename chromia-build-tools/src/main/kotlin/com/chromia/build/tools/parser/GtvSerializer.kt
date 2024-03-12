package com.chromia.build.tools.parser

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvType
import net.postchain.common.toHex
import java.math.BigInteger


class GtvSerializer : JsonSerializer<Gtv>() {
    override fun serialize(gtv: Gtv, generator: JsonGenerator, provider: SerializerProvider?) {
        when (gtv.type) {
            GtvType.NULL -> generator.writeNull()
            GtvType.INTEGER -> generator.writeNumber(gtv.asInteger())
            GtvType.BIGINTEGER -> generator.writeNumber(gtv.asBigInteger())
            GtvType.STRING -> generator.writeString(gtv.asString())
            GtvType.BYTEARRAY -> ByteArraySerializer().serialize(gtv.asByteArray(), generator, provider)
            GtvType.ARRAY -> {
                generator.writeStartArray()
                gtv.asArray().forEach { serialize(it, generator, provider) }
                generator.writeEndArray()
            }

            GtvType.DICT -> {
                generator.writeStartObject()
                gtv.asDict().forEach { (k, v) ->
                    generator.writeFieldName(k)
                    serialize(v, generator, provider)
                }
                generator.writeEndObject()
            }
        }
    }

}

class ByteArraySerializer : JsonSerializer<ByteArray>() {
    override fun serialize(v: ByteArray, generator: JsonGenerator, serializerProvider: SerializerProvider?) {
        v.toHex().let {
            generator.writeNumber("x\"$it\"")
        }
    }

}

class BigIntegerSerializer : JsonSerializer<BigInteger>() {
    override fun serialize(v: BigInteger, generator: JsonGenerator, serializerProvider: SerializerProvider?) {
        generator.writeString("${v}L")
    }
}


