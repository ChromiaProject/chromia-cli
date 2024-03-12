package com.chromia.build.tools.parser

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.NumberDeserializers
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import java.math.BigInteger

class GtvDeserializer : JsonDeserializer<Gtv>() {
    override fun getNullValue(ctxt: DeserializationContext) = GtvNull

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Gtv {

        if (p.currentToken.isBoolean) return gtv(NumberDeserializers.BooleanDeserializer(Boolean::class.java, null).deserialize(p, ctxt))
        return when (p.currentToken) {
            JsonToken.VALUE_NUMBER_FLOAT -> gtv(p.text)
            JsonToken.VALUE_NUMBER_INT -> {
                try {
                    gtv(p.text.toLong())
                } catch (e: Exception) {
                    gtv(p.text.toBigInteger())
                }
            }

            JsonToken.VALUE_STRING -> {
                when {
                    p.text.startsWith("x\"") -> gtv(ByteArrayDeserializer().deserialize(p, ctxt))
                    else -> gtv(p.valueAsString)
                }
            }

            JsonToken.START_ARRAY -> {
                val res = mutableListOf<Gtv>()
                var n: JsonToken? = p.nextToken()
                while (n != JsonToken.END_ARRAY) {
                    res.add(this.deserialize(p, ctxt))
                    n = p.nextToken()
                }
                return gtv(res)
            }

            JsonToken.START_OBJECT -> {
                val res = mutableMapOf<String, Gtv>()
                var n: JsonToken = p.nextToken()
                while (n != JsonToken.END_OBJECT) {
                    val key = p.text
                    p.nextToken()
                    res[key] = this.deserialize(p, ctxt)
                    n = p.nextToken()
                }
                return gtv(res)
            }

            JsonToken.VALUE_NULL -> GtvNull
            else -> gtv(mapOf())
        }
    }
}

class ByteArrayDeserializer : JsonDeserializer<ByteArray>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): ByteArray {
        require(p.valueAsString.startsWith("x\""))
        return p.valueAsString.substringAfter("x\"").trimEnd('"').hexStringToByteArray()
    }
}

class BigIntegerDeserializer : JsonDeserializer<BigInteger>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): BigInteger {
        require(p.valueAsString.endsWith("L"))
        return BigInteger(p.valueAsString.dropLast(1))
    }
}

class WrappedByteArrayDeserializer : JsonDeserializer<WrappedByteArray>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): WrappedByteArray {
        require(p.valueAsString.startsWith("x\""))
        return p.valueAsString.substringAfter("x\"").trimEnd('"').hexStringToWrappedByteArray()
    }
}

