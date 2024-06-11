package com.chromia.cli.tools.env

import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.rell.api.base.RellCliEnv

fun CliktCommand.cliEnv() = CliktCliEnv(this)

class CliktCliEnv(val command: CliktCommand) : RellCliEnv {
    private var seenError = false

    val hasError: Boolean
        get() = seenError

    override fun error(msg: String) {
        seenError = true
        command.echo(msg, err = true)
    }

    override fun print(msg: String) {
        command.echo(msg)
    }
}