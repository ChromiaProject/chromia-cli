package com.chromia.cli.command.node

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class NodeCommand private constructor() : NoOpChromiaCommand(help = "Interact with a test node") {

    companion object {
        fun commands() = NodeCommand().subcommands(
                StartCommand(),
                UpdateCommand()
        )
    }
}
