package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.users.DeveloperAccessLevel
import com.chromia.library.chain.versioning.external.CREATE_ORGANIZATION_INVITATION
import com.chromia.library.chain.versioning.external.createOrganizationInvitationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import kotlin.time.Duration.Companion.minutes

class InviteOrganizationUserCommand : AbstractLibraryCommand(
    name = "invite-user",
    help = "Invite a user to join an organization"
) {

    private val organizationId by argument(
        help = "ID of the library to invite user to"
    )

    private val developerAccountId by argument(
        help = "Account ID of the developer to invite (hex string)"
    )

    private val accessLevel by option(
        "--access-level",
        "-a",
        help = "Access level for the invited user"
    ).choice(
        "admin" to DeveloperAccessLevel.ADMIN,
        "publisher" to DeveloperAccessLevel.PUBLISHER,
        "reviewer" to DeveloperAccessLevel.REVIEWER
    ).default(DeveloperAccessLevel.PUBLISHER)

    private val expiryMs by option(
        "--expiry-ms",
        "-e",
        help = "Expiry duration in milliseconds for invitation"
    ).long()
        .default(10.minutes.inWholeMilliseconds)

    override fun run() = try {
        val accountIdBytes = developerAccountId.hexStringToByteArray()

        authorizeFtAuthOperation(CREATE_ORGANIZATION_INVITATION)

        val res = txBuilder.createOrganizationInvitationOperation(
            organizationId,
            accountIdBytes,
            accessLevel,
            expiryMs
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
                echo("Invitation sent successfully, with transaction rid: ${res.txRid.rid}")
            TransactionStatus.REJECTED ->
                throw PrintMessage("Failed to send invitation: ${res.rejectReason}")
        }
    } catch (e: Exception) {
        echo("Failed to create library invitation: ${e.message}", err = true)
        throw PrintMessage("Library invitation failed")
    }
}
