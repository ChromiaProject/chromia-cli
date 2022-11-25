package com.chromia

import com.chromia.cli.*
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = NoOpCliktCommand()
        .subcommands(
                TestCommand(),
                ReplCommand(),
                StartCommand(),
                DeployCommand(),
                CompileCommand(),
                QueryCommand(),

        )
        .main(args)

//TODO create command to create configfile templates