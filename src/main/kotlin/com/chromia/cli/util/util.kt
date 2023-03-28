package com.chromia.cli.util

enum class LanguageSupport {
    Kotlin,
    Typescript,
    Javascript;

    fun flag() = "--${name.lowercase()}"
}
