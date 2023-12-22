package com.chromia

import com.chromia.cli.BuildCommand
import com.chromia.cli.CreateRellDappCommand
import com.chromia.cli.GenerateClientStubsCommand
import com.chromia.cli.InstallCommand
import com.chromia.cli.KeygenCommand
import com.chromia.cli.QueryCommand
import com.chromia.cli.ReplCommand
import com.chromia.cli.TestCommand
import com.chromia.cli.TxCommand
import com.chromia.cli.deployCommands
import com.chromia.cli.eifCommands
import com.chromia.cli.nodeCommands
import com.chromia.cli.tools.launcher.CliLauncher
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import net.postchain.PostchainNode
import net.postchain.eif.EifGTXModule
import net.postchain.rell.base.utils.RellVersions

fun main(args: Array<out String>) = CliLauncher("chr")
        .versionOption("""
            ${CliLauncher::class.java.`package`.implementationVersion}
            rell version ${RellVersions::class.java.`package`.implementationVersion ?: "(unknown)"}
            postchain version ${PostchainNode::class.java.`package`.implementationVersion ?: "(unknown)"}
            EIF version ${EifGTXModule::class.java.`package`.implementationVersion ?: "(unknown)"}
            Java version ${System.getProperty("java.version")}
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
                eifCommands()
        )
        .catchingAllExceptionsMain(args)
