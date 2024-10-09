package com.chromia.cli.command.generate

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.subcommands

class GenerateCommand private constructor(): NoOpChromiaCommand(help = "Generate client stubs and documentation for a rell project") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = GenerateCommand().subcommands(
                GenerateClientStubsCommand(),
                GenerateMermaidGraphCommand(),
                GenerateDocsSiteCommand(),
        )
    }
}