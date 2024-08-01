package com.chromia

import com.chromia.cli.command.BuildCommand
import com.chromia.cli.command.CreateRellDappCommand
import com.chromia.cli.command.FormatCommand
import com.chromia.cli.command.InstallCommand
import com.chromia.cli.command.KeygenCommand
import com.chromia.cli.command.LintCommand
import com.chromia.cli.command.QueryCommand
import com.chromia.cli.command.ReplCommand
import com.chromia.cli.command.TestCommand
import com.chromia.cli.command.TxCommand
import com.chromia.cli.command.code.CodeCommand
import com.chromia.cli.command.deployment.DeploymentCommand
import com.chromia.cli.command.eif.EifCommand
import com.chromia.cli.command.generate.GenerateCommand
import com.chromia.cli.command.node.NodeCommand
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
                BuildCommand(),
                CreateRellDappCommand(),
                DeploymentCommand.commands(),
                EifCommand.commands(),
                GenerateCommand.commands(),
                InstallCommand(),
                KeygenCommand(),
                NodeCommand.commands(),
                QueryCommand(),
                ReplCommand(),
                TestCommand(),
                TxCommand(),
                CodeCommand.commands(),
        )
        .catchingAllExceptionsMain(args)
