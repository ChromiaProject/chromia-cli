package com.chromia

import com.chromia.cli.*
import com.chromia.cli.util.createAliases
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import net.postchain.PostchainNode
import net.postchain.rell.module.RellVersions

fun main(args: Array<out String>) = object : NoOpCliktCommand(name = "chr") {
    init {
        completionOption()
        versionOption("""
            ${this::class.java.`package`.implementationVersion}
            rell version ${RellVersions::class.java.`package`.implementationVersion ?: "(unknown)"}
            postchain version ${PostchainNode::class.java.`package`.implementationVersion ?: "(unknown)"}
        """.trimIndent())
    }

    override fun aliases() = createAliases()
}
        .subcommands(
                CreateRellDappCommand(),
                TestCommand(),
                GenerateClientStubsCommand(),
                ReplCommand(),
                nodeCommands(),
                deployCommands(),
                BuildCommand().apply { subcommands(BuildInfoCommand()) },
                QueryCommand(),
                TxCommand(),
                KeygenCommand(),
                //InstallCommand(),
        )
        .main(args)
