package com.chromia.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class DeploymentCommand: NoOpCliktCommand(help = "Create and maintain deployments")

fun deployCommands() = DeploymentCommand().subcommands(
        DeployCreateCommand(),
        DeployInfoCommand(),
)