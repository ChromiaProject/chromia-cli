package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.ACCEPT_LIBRARY_INVITATION
import com.chromia.library.chain.versioning.external.acceptLibraryInvitationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import net.postchain.common.tx.TransactionStatus

class AcceptLibraryInvitationCommand : AbstractLibraryCommand(
    name = "accept-invitation",
    help = "Accept a library invitation"
) {
    override val hiddenFromHelp = true

    val invitationCode by argument(
        help = "The invitation code to accept"
    )

    override fun run() {
        authorizeFtAuthOperation(ACCEPT_LIBRARY_INVITATION)

        val res = txBuilder.acceptLibraryInvitationOperation(invitationCode).run {
            addNop()
            postAwaitConfirmation(txListener())
        }

        when (res.status) {
            TransactionStatus.UNKNOWN ->
                echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")

            TransactionStatus.WAITING ->
                echo("transaction with rid ${res.txRid.rid} was posted but is still pending")

            TransactionStatus.CONFIRMED ->
                echo("Invitation accepted successfully in transaction rid: ${res.txRid.rid}")

            TransactionStatus.REJECTED ->
                throw PrintMessage("Invitation acceptance failed: ${res.status} -> ${res.rejectReason}")
        }
    }
}
