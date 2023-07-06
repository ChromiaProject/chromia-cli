package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.rell.api.base.RellCliEnv


class CliktCliEnv(val command: CliktCommand) : RellCliEnv() {
    override fun error(msg: String) {
        command.currentContext.terminal.println(msg, stderr = true)
    }

    override fun print(msg: String) {
        command.currentContext.terminal.println(msg)
    }
}
