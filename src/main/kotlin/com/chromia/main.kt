package com.chromia

import com.chromia.cli.*
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = NoOpCliktCommand(name = "chr")
        .completionOption()
        .subcommands(
                InitCommand(),
                TestCommand(),
                ReplCommand(),
                StartCommand(),
                DeployCommand(),
                BuildCommand(),
                QueryCommand(),
                TxCommand(),
        )
        .main(args)

//TODO create command to create configfile templates