package com.chromia.cli.command.library.management

import com.chromia.cli.command.NoOpChromiaCommand
import com.github.ajalt.clikt.core.subcommands

class LibraryManagementCommand private constructor() : NoOpChromiaCommand(help = "Manage libraries") {

    companion object {
        fun commands() = LibraryManagementCommand().subcommands(
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
            InstallLibraryCommand()
        )
    }
}
