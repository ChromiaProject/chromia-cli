package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.users.DeveloperAccessLevel
import com.chromia.library.chain.versioning.external.UPDATE_LIBRARY_DEVELOPER_ACCESS_LEVEL
import com.chromia.library.chain.versioning.external.updateLibraryDeveloperAccessLevelOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus

class UpdateLibraryUserPermissionCommand : AbstractLibraryCommand(
    name = "update-user-permission",
    help = "Update a user's permission level for a library"
) {
    override val hiddenFromHelp = true

    private val libraryId by argument(
        help = "ID of the library"
    )

    private val developerAccountId by argument(
        help = "Account ID of the developer (hex string)"
    )

    private val accessLevel by option(
        "--access-level",
        "-a",
        help = "New access level for the user"
    ).choice(
        "admin" to DeveloperAccessLevel.ADMIN,
        "publisher" to DeveloperAccessLevel.PUBLISHER,
        "reviewer" to DeveloperAccessLevel.REVIEWER
    ).required()

    override fun run() {
        try {
            val accountIdBytes = developerAccountId.hexStringToByteArray()
            authorizeFtAuthOperation(UPDATE_LIBRARY_DEVELOPER_ACCESS_LEVEL)

            val res = txBuilder.updateLibraryDeveloperAccessLevelOperation(
                libId = libraryId,
                toDevId = accountIdBytes,
                accessLevel = accessLevel
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
                    echo("Developer permissions updated successfully, in transaction ${res.txRid.rid} with new access level: $accessLevel")
                TransactionStatus.REJECTED ->
                    throw PrintMessage(
                            "Unable to update developer's permission with status -> ${res.status}: ${res.rejectReason}",
                            statusCode = 1
                    )
            }
        } catch (e: Exception) {
            echo("Failed to update developer permission: ${e.message}", err = true)
            throw PrintMessage("Permission update failed")
        }
    }
}
