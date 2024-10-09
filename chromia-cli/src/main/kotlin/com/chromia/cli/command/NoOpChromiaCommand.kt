package com.chromia.cli.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand

abstract class NoOpChromiaCommand(name: String? = null, private val help: String): NoOpCliktCommand(name) {
    override fun help(context: Context) = help
}
