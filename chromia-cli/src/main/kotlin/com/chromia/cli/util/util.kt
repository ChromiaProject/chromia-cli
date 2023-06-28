package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand

enum class LanguageSupport {
    Kotlin,
    Typescript,
    Javascript;

    fun flag() = "--${name.lowercase()}"
}

fun CliktCommand.createAliases(): Map<String, List<String>> {
    val prefixCounts = mutableMapOf<String, Int>().withDefault { 0 }
    val prefixes = mutableMapOf<String, List<String>>()
    for (name in registeredSubcommandNames()) {
        if (name.length < 3) continue
        for (i in 1..name.lastIndex) {
            val prefix = name.substring(0..i)
            prefixCounts[prefix] = prefixCounts.getValue(prefix) + 1
            prefixes[prefix] = listOf(name)
        }
    }
    return prefixes.filterKeys { prefixCounts.getValue(it) == 1 }
}