package com.chromia.cli

import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class GenerateCommand: NoOpCliktCommand(help = "Generate client stubs and documentation for a rell project") {
    override fun aliases() = createAliases()

    companion object {
        fun generateCommands() = GenerateCommand().subcommands(
                GenerateClientStubsCommand(),
                GenerateMermaidGraphCommand(),
                GenerateDocsSiteCommand(),
        )
    }
}