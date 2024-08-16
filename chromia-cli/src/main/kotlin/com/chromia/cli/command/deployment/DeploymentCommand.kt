package com.chromia.cli.command.deployment

import com.chromia.cli.command.deployment.proposal.ProposalCommand
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

class DeploymentCommand private constructor() : NoOpCliktCommand(help = "Create and maintain deployments") {
    override fun aliases() = createAliases()

    companion object {
        fun commands() = DeploymentCommand().subcommands(
                DeployCreateCommand(),
                DeployInfoCommand(),
                DeployInspectCommand(),
                DeployUpdateCommand(),
                DeployResumeCommand(),
                DeployPauseCommand(),
                DeployRemoveCommand(),
                DeployInspectLeaseCommand(),
                ProposalCommand.commands()
        )
    }
}
