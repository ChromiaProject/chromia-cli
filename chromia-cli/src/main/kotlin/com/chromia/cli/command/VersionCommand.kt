package com.chromia.cli.command

import com.github.ajalt.clikt.core.PrintMessage

class VersionCommand(private val command: String, private val version: String) :
        ChromiaCommand("version", "Show the version and exit") {
    override fun run() {
        throw PrintMessage("$command version $version")
    }
}
