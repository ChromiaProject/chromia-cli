package com.chromia

import com.chromia.cli.*
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class chr: NoOpCliktCommand() { //TODO rename when we decide on name for cli
}

fun main(args: Array<String>) = chr()
    .subcommands(
            TestCommand(),
            ReplCommand(),
            StartCommand(),
            DeployCommand(),
            UpdateCommand(),
            CompileCommand(),
    )
    .main(args)

//TODO create command to create configfile templates