package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import net.postchain.rell.utils.RellCliEnv


class CliktCliEnv(val command: CliktCommand) : RellCliEnv() {
    override fun exit(status: Int) = throw ProgramResult(status)
    override fun print(msg: String, err: Boolean) = command.currentContext.console.print(msg, err)
}
