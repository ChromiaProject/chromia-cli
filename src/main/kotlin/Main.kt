package com.example


import com.example.rell.Rell
import com.example.rell.RunTest
import com.example.rell.rellCommands
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.types.int

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