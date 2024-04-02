package com.chromia.cli.command.node

import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class NodeCommand private constructor(): NoOpCliktCommand(help = "Interact with a test node") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = NodeCommand().subcommands(
                StartCommand(),
                UpdateCommand()
        )
    }
}
