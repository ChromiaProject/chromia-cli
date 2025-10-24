package com.chromia.cli.command.deployment

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.command.deployment.proposal.ProposalCommand
import com.chromia.cli.command.deployment.voterset.VotersetCommand
import com.github.ajalt.clikt.core.subcommands

class DeploymentCommand private constructor() : NoOpChromiaCommand(help = "Create and maintain deployments") {
    override fun aliases() =
        mapOf(
            "pause-container" to listOf("container", "pause"),
            "resume-container" to listOf("container", "resume")
        )

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
                ProposalCommand.commands(),
                VotersetCommand.commands(),
                ContainerCommand.commands(),
                RemoveContainerCommand(),
        )
    }
}
