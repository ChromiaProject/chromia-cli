package com.chromia.cli.command.multisignature

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class MultiSignatureCommand : NoOpChromiaCommand(help = "Handle transactions with need of multiple signers") {

    companion object {
        fun commands() = MultiSignatureCommand().subcommands(
                MultiSignatureCreateCommand(),
                MultiSignatureSignCommand(),
                MultiSignatureSendCommand(),
                MultiSignatureViewCommand()
        )
    }

}
