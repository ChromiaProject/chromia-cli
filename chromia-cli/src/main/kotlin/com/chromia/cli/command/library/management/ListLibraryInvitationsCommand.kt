package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.TypesSActiveInvitation
import com.chromia.library.chain.versioning.external.GET_PENDING_INVITATIONS_FOR_DEVELOPER
import com.chromia.library.chain.versioning.external.getPendingInvitationsForDeveloper
import com.github.ajalt.clikt.core.PrintMessage

class ListLibraryInvitationsCommand : AbstractLibraryCommand(
    name = "list-invitations",
    help = "List pending invitations"
) {
    override val hiddenFromHelp = true

    override fun run() {
        try {
            val (accountId, _) = authorizeFtAuthOperation(GET_PENDING_INVITATIONS_FOR_DEVELOPER)

            val invitations = client.getPendingInvitationsForDeveloper(accountId)

            invitations.takeIf { it.isNotEmpty() }?.let {
                displayInvitations(invitations)
            } ?: echo("No pending invitations")
        } catch (e: Exception) {
            echo("Failed to list invitations: ${e.message}", err = true)
            throw PrintMessage("Library invitation listing failed")
        }
    }

    private fun displayInvitations(invitations: List<TypesSActiveInvitation>) {
        echo("Pending Invitations:")
        invitations.forEach { invitation ->
            val code = invitation.code
            val libraryId = invitation.libraryId
            val expiresAt = invitation.expiresAt
            val organizationId = invitation.organizationId
            echo(
                "• Code: $code | Library: $libraryId | Expires: $expiresAt | Organization: $organizationId"
            )
        }
        echo("Total: ${invitations.size} invitation(s)")
    }
}
