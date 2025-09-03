package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.DELETE_DEVELOPER_FROM_LIBRARY
import com.chromia.library.chain.versioning.external.deleteDeveloperFromLibraryOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus

class RemoveLibraryUserCommand : AbstractLibraryCommand(
    name = "delete-dev",
    help = "Delete a developer from a library",
    hideKeyPairSourceHelpMessage = true
) {
    override val hiddenFromHelp = true

    private val libraryId by option(
        "--library-id",
        help = "ID of the library to remove user from"
    ).required()

    private val devAccountId by option(
        "--dev-id",
        help = "Account ID of the developer to remove"
    ).required()

    override fun run() {
        try {
            authorizeFtAuthOperation(DELETE_DEVELOPER_FROM_LIBRARY)

            val res = txBuilder.deleteDeveloperFromLibraryOperation(
                libraryId = libraryId,
                accountId = devAccountId.hexStringToByteArray()
            ).run {
                addNop()
                postAwaitConfirmation(txListener())
            }

            when (res.status) {
                TransactionStatus.UNKNOWN ->
                    echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")
                TransactionStatus.WAITING ->
                    echo("transaction with rid ${res.txRid.rid} was posted but is still pending")
                TransactionStatus.CONFIRMED ->
                    echo("User removed from library successfully")
                TransactionStatus.REJECTED ->
                    throw PrintMessage(
                        "Failed to remove user: ${res.rejectReason}",
                        statusCode = 1
                    )
            }
        } catch (e: Exception) {
            echo("Failed to remove library user: ${e.message}", err = true)
            throw PrintMessage("Library user removal failed")
        }
    }
}
