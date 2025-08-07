package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.ACCEPT_ORGANIZATION_INVITATION
import com.chromia.library.chain.versioning.external.acceptOrganizationInvitationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import net.postchain.common.tx.TransactionStatus

class AcceptInvitationCommand : AbstractLibraryCommand(
    name = "accept-invitation",
    help = "Accept an organization invitation"
) {

    private val invitationCode by argument(
        help = "The invitation code to accept"
    )

    override fun run() {
        try {
            authorizeFtAuthOperation(ACCEPT_ORGANIZATION_INVITATION)

            val res = txBuilder.acceptOrganizationInvitationOperation(
                invitationCode = invitationCode
            ).run {
                addNop()
                postAwaitConfirmation(txListener())
            }

            when (res.status) {
                TransactionStatus.UNKNOWN ->
                    echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")
                TransactionStatus.WAITING ->
                    echo("transaction with rid ${res.txRid.rid} was posted but is still pending")
                TransactionStatus.CONFIRMED -> {
                    echo("Invitation Code: $invitationCode")
                    echo("Organization invitation accepted successfully!")
                    echo("You are now a member of the organization.")
                }
                TransactionStatus.REJECTED -> {
                    echo("Failed to accept organization invitation, with transaction: ${res.txRid} reason: ${res.rejectReason}", err = true)
                    throw PrintMessage(
                        "Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}",
                        statusCode = 1
                    )
                }
            }
        } catch (e: Exception) {
            throw PrintMessage("Invitation acceptance failed\n${e.message}")
        }
    }
}
