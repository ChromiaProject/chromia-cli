package com.example.rell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class Rell: NoOpCliktCommand(help = "Rell commands")

fun rellCommands() = Rell()
    .subcommands(RunTest())
    //TODO create command to create configfile templates