package com.chromia.cli


import com.example.com.chromia.cli.compile.rellCommands
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class chr: NoOpCliktCommand() {
/*    override fun aliases(): Map<String, List<String>> {
        return mapOf(
            "test" to listOf("rell", "run-test")
        )
    }*/
}

fun main(args: Array<String>) = chr()
    .subcommands(
        rellCommands(),
    )
    .main(args)