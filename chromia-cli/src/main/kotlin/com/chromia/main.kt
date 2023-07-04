package com.chromia

import com.chromia.cli.*
import com.chromia.cli.tools.launcher.CliLauncher
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import net.postchain.PostchainNode
import net.postchain.rell.base.utils.RellVersions

fun main(args: Array<out String>) = CliLauncher("chr")
        .versionOption("""
            ${CliLauncher::class.java.`package`.implementationVersion}
            rell version ${RellVersions::class.java.`package`.implementationVersion ?: "(unknown)"}
            postchain version ${PostchainNode::class.java.`package`.implementationVersion ?: "(unknown)"}
        """.trimIndent())
        .subcommands(
                CreateRellDappCommand(),
                TestCommand(),
                GenerateClientStubsCommand(),
                ReplCommand(),
                nodeCommands(),
                deployCommands(),
                BuildCommand(),
                QueryCommand(),
                TxCommand(),
                KeygenCommand(),
                InstallCommand(),
        )
        .catchingAllExceptionsMain(args)
