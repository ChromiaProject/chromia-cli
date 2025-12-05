package com.chromia.cli.command.generate

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class GenerateCommand private constructor() : NoOpChromiaCommand(help = "Generate client stubs, documentation, and entity relations for a rell project") {

    companion object {
        fun commands() = GenerateCommand().subcommands(
                GenerateClientStubsCommand(),
                GenerateMermaidGraphCommand(),
                GenerateDocsSiteCommand(),
        )
    }
}