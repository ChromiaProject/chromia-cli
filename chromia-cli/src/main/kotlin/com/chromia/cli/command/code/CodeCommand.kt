package com.chromia.cli.command.code

import com.chromia.cli.command.FormatCommand
import com.chromia.cli.command.LintCommand
import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class CodeCommand private constructor() : NoOpChromiaCommand(help = "Code quality management (formatting, linting)") {

    companion object {
        fun commands() = CodeCommand().subcommands(
                LintCommand(),
                FormatCommand(),
        )
    }
}
