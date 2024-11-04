package com.chromia.cli.util

import com.google.gson.GsonBuilder
import net.postchain.common.toHex
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvAdapter
import net.postchain.gtv.GtvType
import net.postchain.gtv.gtvml.GtvMLEncoder
import net.postchain.gtv.yaml.GtvYaml

private val PRETTY_GSON = GsonBuilder()
        .registerTypeAdapter(Gtv::class.java, GtvAdapter(strict = false))
        .serializeNulls()
        .setPrettyPrinting()
        .create()!!

fun formatJson(gtv: Gtv): String = PRETTY_GSON.toJson(gtv, Gtv::class.java)

fun formatXml(gtv: Gtv): String {
    val xml = GtvMLEncoder.encodeXMLGtv(gtv)
    return xml.removePrefix("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                    |""".trimMargin()).dropLast(1)
}

fun formatYaml(gtv: Gtv): String = GtvYaml().dump(gtv).dropLast(1)

fun formatRaw(gtv: Gtv): String = when (gtv.type) {
    GtvType.NULL -> "null"
    GtvType.BYTEARRAY -> "0x${gtv.asByteArray().toHex().lowercase()}"
    GtvType.STRING -> gtv.asString()
    GtvType.INTEGER -> gtv.asInteger().toString()
    GtvType.BIGINTEGER -> gtv.asBigInteger().toString()
    GtvType.ARRAY -> gtv.asArray().joinToString("\n") { formatRaw(it) }
    GtvType.DICT -> gtv.asDict().entries.joinToString("\n") { "${it.key}=${formatRaw(it.value)}" }
}
