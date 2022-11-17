package com.chromia.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class Rell: NoOpCliktCommand(help = "Rell commands")

fun rellCommands() = Rell()
    .subcommands(TestCommand())
    //TODO create command to create configfile templates