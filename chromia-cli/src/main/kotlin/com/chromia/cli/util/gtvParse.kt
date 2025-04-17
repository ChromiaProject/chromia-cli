package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktError
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser

val GtvPattern = Regex("""^(["'\[x0-9-]|null|true|false).*""")

/**
 * Parses the given string as a GTV (Generic Typed Value) object. If the string is non-blank
 * and starts with a recognized character or sequence indicating a valid GTV object (e.g., `"`, `'`, `[`, `x`,
 * or specific keywords like "null", "true", "false"), it is parsed using the `GtvParser`.
 * Otherwise, it is interpreted as a raw string wrapped in a `GtvString`.
 *
 * @param s The input string to be parsed as a GTV object. Must not be blank and should follow
 * the expected GTV content format or syntax.
 * @return A `Gtv` object parsed from the input string, or a `GtvString` if it does not start like a valid GTV expression
 * @throws IllegalArgumentException If the input string is blank or starts like a valid GTV expression but does not
 * follow the expected GTV format.
 */
fun parseArgAsGtv(s: String): Gtv = if (GtvPattern.matches(s)) {
    try {
        GtvParser.parse(s)
    } catch (e: IllegalArgumentException) {
        throw CliktError("Invalid GTV expression <$s>: ${e.message ?: "Unknown error"}")
    }
} else {
    GtvString(s)
}
