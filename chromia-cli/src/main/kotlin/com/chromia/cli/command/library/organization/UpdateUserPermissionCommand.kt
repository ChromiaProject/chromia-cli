package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.users.DeveloperAccessLevel
import com.chromia.library.chain.versioning.external.UPDATE_ORGANIZATION_DEVELOPER_ACCESS_LEVEL
import com.chromia.library.chain.versioning.external.updateOrganizationDeveloperAccessLevelOperation
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus

class UpdateUserPermissionCommand : AbstractLibraryCommand(
    name = "update-permission",
    help = "Update a user's permission level in an organization"
) {
    private val organizationId by argument(
        help = "ID of the organization"
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

    override fun run() = try {
        val accountIdBytes = developerAccountId.hexStringToByteArray()

        authorizeFtAuthOperation(UPDATE_ORGANIZATION_DEVELOPER_ACCESS_LEVEL)

        val res = txBuilder.updateOrganizationDeveloperAccessLevelOperation(
            orgId = organizationId,
            toDevId = accountIdBytes,
            accessLevel = accessLevel
        ).run {
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
        throw CliktError("Permission update failed")
    }
}
