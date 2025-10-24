package com.chromia.cli.command.deployment

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class ContainerCommand private constructor() : NoOpChromiaCommand(
    name = "container",
    help = "Manage container operations"
) {

    companion object {
        fun commands() = ContainerCommand().subcommands(
            ContainerConfigurationCommand(),
            PauseContainerCommand(),
            ResumeContainerCommand(),
        )
    }
}
