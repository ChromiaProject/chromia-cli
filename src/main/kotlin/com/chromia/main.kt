package com.chromia

import com.chromia.cli.*
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

fun main(args: Array<String>) = NoOpCliktCommand()
        .completionOption()
        .subcommands(
                InitCommand(),
                TestCommand(),
                ReplCommand(),
                StartCommand(),
                DeployCommand(),
                CompileCommand(),
                QueryCommand(),
                TxCommand(),
        )
        .main(args)

//TODO create command to create configfile templates