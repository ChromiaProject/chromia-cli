package com.chromia

import com.chromia.cli.rellCommands
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class chr: NoOpCliktCommand() { //TODO rename when we decide on name for cli
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