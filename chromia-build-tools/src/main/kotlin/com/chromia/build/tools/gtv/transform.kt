package com.chromia.build.tools.gtv

import net.postchain.gtv.GtvDictionary

fun GtvDictionary.remove(head: String, vararg tail: String): GtvDictionary = if (tail.isNotEmpty()) {
    val value = dict[head]
    if (value is GtvDictionary)
        GtvDictionary.build(this.dict.toMutableMap().apply {
            put(head, value.remove(tail.first(), *tail.drop(1).toTypedArray()))
        }.toMap())
    else this
} else
    GtvDictionary.build(this.dict.toMutableMap().apply { remove(head) }.toMap())
