package com.chromia.build.tools.parser

import net.postchain.common.toHex

fun mapByteArraysToHexString(input: Any): Any {
    return when (input) {
        is Map<*, *> -> {
            input.mapValues { it.value?.let { it1 -> mapByteArraysToHexString(it1) } }
        }

        is List<*> -> {
            input.map { it?.let { it1 -> mapByteArraysToHexString(it1) } }
        }

        is ByteArray -> {
            "x\"${input.toHex()}\""
        }

        else -> input
    }
}
