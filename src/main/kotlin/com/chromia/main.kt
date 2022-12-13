package com.chromia

import com.chromia.cli.*
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import net.postchain.PostchainNode
import net.postchain.rell.module.RellVersions
import net.postchain.cli.CommandKeygen

fun main(args: Array<out String>) = object : NoOpCliktCommand(name = "chr") {
    init {
        versionOption("""
            ${this::class.java.`package`.implementationVersion}
            rell version ${RellVersions::class.java.`package`.implementationVersion ?: "(unknown)"}
            postchain version ${PostchainNode::class.java.`package`.implementationVersion ?: "(unknown)"}
        """.trimIndent())
    }
}
        .subcommands(
                InitCommand(),
                TestCommand(),
                ReplCommand(),
                StartCommand(),
                DeployCommand(),
                BuildCommand(),
                QueryCommand(),
                TxCommand(),
                CommandKeygen(),
        )
        .main(args)