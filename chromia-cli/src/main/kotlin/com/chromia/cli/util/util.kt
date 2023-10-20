package com.chromia.cli.util


enum class LanguageSupport {
    Kotlin,
    Typescript,
    Javascript,
    Mermaid;

    fun flag() = "--${name.lowercase()}"
}
