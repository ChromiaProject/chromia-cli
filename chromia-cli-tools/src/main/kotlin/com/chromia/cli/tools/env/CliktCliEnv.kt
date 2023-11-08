package com.chromia.cli.tools.env

import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.rell.api.base.RellCliEnv


fun CliktCommand.cliEnv() = CliktCliEnv(this)
class CliktCliEnv(val command: CliktCommand) : RellCliEnv {
    override fun error(msg: String) = command.currentContext.terminal.println(msg, stderr = true)
    override fun print(msg: String) = command.currentContext.terminal.println(msg)
}