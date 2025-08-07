package com.chromia.cli.command.library.developer

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.users.external.UPDATE_DEVELOPER_NAME
import com.chromia.library.chain.users.external.updateDeveloperNameOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import net.postchain.common.toHex
import net.postchain.common.tx.TransactionStatus

class UpdateDeveloperCommand : AbstractLibraryCommand("update", help = "Update a library developer's name") {

    val newName by argument(help = "New name of the library developer")

    override fun run() {
        val (accountId, _) = authorizeFtAuthOperation(UPDATE_DEVELOPER_NAME)

        val res = txBuilder.updateDeveloperNameOperation(newName).run {
            addNop()
            postAwaitConfirmation(txListener())
        }

        when (res.status) {
            TransactionStatus.UNKNOWN ->
                echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")

            TransactionStatus.WAITING ->
                echo("transaction with rid ${res.txRid.rid} was posted but is still pending")

            TransactionStatus.CONFIRMED ->
                echo(
                    "Developer with id '${accountId.toHex()}' updated successfully in transaction ${res.txRid} with new name '$newName'"
                )

            TransactionStatus.REJECTED ->
                throw PrintMessage(
                    "Transaction Failed with code ${res.httpStatusCode}: ${res.rejectReason}",
                    statusCode = 1
                )
        }
    }
}
