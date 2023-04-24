package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.rell.utils.cli.RellCliEnv


class CliktCliEnv(val command: CliktCommand) : RellCliEnv() {
    override fun error(msg: String) {
        command.currentContext.console.print(msg + command.currentContext.console.lineSeparator, true)
    }

    override fun print(msg: String) {
        command.currentContext.console.print(msg + command.currentContext.console.lineSeparator, false)
    }
}
