package com.chromia.cli.command.library.organization

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class OrganizationCommand private constructor() : NoOpChromiaCommand(help = "Manage organizations") {
    override val hiddenFromHelp = true

    companion object {
        fun commands() = OrganizationCommand().subcommands(
            CreateOrganizationCommand(),
            ListOrganizationsCommand(),
            RemoveUserCommand(),
            UpdateUserPermissionCommand(),
            ViewOrganizationCommand(),
            AcceptInvitationCommand(),
            InviteOrganizationUserCommand()
        )
    }
}
