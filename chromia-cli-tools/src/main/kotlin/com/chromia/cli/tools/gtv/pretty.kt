package com.chromia.cli.tools.gtv

import net.postchain.gtv.ESCAPE_GTV
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary

const val INDENT = 2

fun Gtv.pretty(nesting: Int = 0): String = when (this) {
    is GtvArray -> prettyArray(this, nesting)
    is GtvDictionary -> prettyDict(this, nesting)
    else -> toString()
}

private fun prettyArray(gtv: GtvArray, nesting: Int) =
        if (gtv.array.isEmpty())
            indent(nesting) + "[]"
        else
            gtv.array.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n" + indent(nesting) + "]") {
                indent(nesting + 1) + it.pretty(nesting + 1)
            }

private fun prettyDict(gtv: GtvDictionary, nesting: Int) =
        if (gtv.dict.isEmpty())
            indent(nesting) + "[:]"
        else
            gtv.dict.asIterable().joinToString(separator = ",\n", prefix = "[\n", postfix = "\n" + indent(nesting) + "]") {
                indent(nesting + 1) + "\"${ESCAPE_GTV.translate(it.key)}\": ${it.value.pretty(nesting + 1)}"
            }

private fun indent(nesting: Int) = " ".repeat(nesting * INDENT)
