package com.chromia

import com.chromia.cli.command.BuildCommand
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.CreateRellDappCommand
import com.chromia.cli.command.FetchConfigCommand
import com.chromia.cli.command.HelpCommand
import com.chromia.cli.command.KeygenCommand
import com.chromia.cli.command.QueryCommand
import com.chromia.cli.command.ReplCommand
import com.chromia.cli.command.TestCommand
import com.chromia.cli.command.TxCommand
import com.chromia.cli.command.VersionCommand
import com.chromia.cli.command.code.CodeCommand
import com.chromia.cli.command.deployment.DeploymentCommand
import com.chromia.cli.command.eif.EifCommand
import com.chromia.cli.command.generate.GenerateCommand
import com.chromia.cli.command.library.LibraryCommand
import com.chromia.cli.command.multisignature.MultiSignatureCommand
import com.chromia.cli.command.node.NodeCommand
import com.chromia.cli.command.seeder.SeederCommand
import com.chromia.cli.command.tools.ToolsCommand
import com.chromia.cli.tools.launcher.CliLauncher
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.versionOption
import net.postchain.PostchainNode
import net.postchain.eif.EifGTXModule
import net.postchain.rell.base.utils.RellVersions

fun main(args: Array<out String>) {
    val commandName = "chr"
    val version = """
            ${ChromiaCommand::class.java.`package`.implementationVersion ?: "(unknown)"}
            rell version ${RellVersions::class.java.`package`.implementationVersion ?: "(unknown)"}
            postchain version ${PostchainNode::class.java.`package`.implementationVersion ?: "(unknown)"}
            EIF version ${EifGTXModule::class.java.`package`.implementationVersion ?: "(unknown)"}
            Java version ${System.getProperty("java.version")}
        """.trimIndent()
    return object : CliLauncher(commandName) {
        override fun aliases() =
                mapOf(
                        "generate-client-stubs" to listOf("generate", "client-stubs"),  // Deprecated alias
                        "gtv" to listOf("tools", "gtv"),
                        "install" to listOf("library", "install"),
                )
    }.versionOption(version)
            .subcommands(
                    HelpCommand(),
                    VersionCommand(commandName, version),
                    BuildCommand(),
                    CreateRellDappCommand(),
                    DeploymentCommand.commands(),
                    EifCommand.commands(),
                    GenerateCommand.commands(),
                    KeygenCommand(),
                    NodeCommand.commands(),
                    QueryCommand(),
                    ReplCommand(),
                    TestCommand(),
                    TxCommand(),
                    CodeCommand.commands(),
                    FetchConfigCommand(),
                    MultiSignatureCommand.commands(),
                    ToolsCommand.commands(),
                    SeederCommand.commands(),
                    LibraryCommand.commands()
            )
            .catchingAllExceptionsMain(args)
}
