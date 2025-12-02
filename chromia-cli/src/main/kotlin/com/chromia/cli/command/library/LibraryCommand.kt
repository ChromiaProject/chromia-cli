package com.chromia.cli.command.library

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.command.library.developer.DeveloperCommand
import com.chromia.cli.command.library.management.AcceptLibraryInvitationCommand
import com.chromia.cli.command.library.management.CreateLibraryCommand
import com.chromia.cli.command.library.management.DeployNewLibraryVersionCommand
import com.chromia.cli.command.library.management.InstallLibraryCommand
import com.chromia.cli.command.library.management.InviteLibraryUserCommand
import com.chromia.cli.command.library.management.ListLibrariesCommand
import com.chromia.cli.command.library.management.ListLibraryInvitationsCommand
import com.chromia.cli.command.library.management.ListVersionsCommand
import com.chromia.cli.command.library.management.RemoveLibraryUserCommand
import com.chromia.cli.command.library.management.UpdateLibraryDescription
import com.chromia.cli.command.library.management.UpdateLibraryUserPermissionCommand
import com.chromia.cli.command.library.management.ViewLibraryCommand
import com.chromia.cli.command.library.organization.OrganizationCommand
import com.github.ajalt.clikt.core.subcommands

class LibraryCommand private constructor() : NoOpChromiaCommand(
    help = "Manage organizations and libraries in the Chromia library ecosystem"
) {

    companion object {
        fun commands() = LibraryCommand().subcommands(
            OrganizationCommand.commands(),
            DeveloperCommand.commands(),
            InstallLibraryCommand(),
            CreateLibraryCommand(),
            ListLibrariesCommand(),
            ViewLibraryCommand(),
            DeployNewLibraryVersionCommand(),
            ListVersionsCommand(),
            InviteLibraryUserCommand(),
            RemoveLibraryUserCommand(),
            UpdateLibraryUserPermissionCommand(),
            AcceptLibraryInvitationCommand(),
            ListLibraryInvitationsCommand(),
            UpdateLibraryDescription()
        )
    }
}
