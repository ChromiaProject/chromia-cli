package com.chromia.cli.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.PrintHelpMessage

class HelpCommand: ChromiaCommand("help", "Show this message and exit") {
    override fun run() {
        throw PrintHelpMessage(null)
    }
}
