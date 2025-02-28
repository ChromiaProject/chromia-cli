package com.chromia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.installMordantMarkdown
import java.io.File

abstract class ChromiaCommand(name: String? = null, private val help: String): CliktCommand(name) {
    override fun help(context: Context) = help
    init {
        installMordantMarkdown()
    }
}

sealed class KeyPairSource {
    data class KeyId(val name: String) : KeyPairSource()
    data class SecretFile(val file: File) : KeyPairSource()
}
