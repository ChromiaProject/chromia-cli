package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.users.DeveloperAccessLevel
import com.chromia.library.chain.versioning.external.CREATE_LIBRARY_INVITATION
import com.chromia.library.chain.versioning.external.createLibraryInvitationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import kotlin.time.Duration.Companion.minutes

class InviteLibraryUserCommand : AbstractLibraryCommand(
    name = "invite-user",
    help = "Invite a user to collaborate on a library"
) {
    override val hiddenFromHelp = true

    private val libraryId by option(
        "--library-id",
        help = "ID of the library to invite user to"
    ).required()

    private val developerAccountId by option(
        "--developer-id",
        help = "Account ID of the developer to invite (hex string)"
    ).required()

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
        val devAccountBytes = developerAccountId.hexStringToByteArray()

        authorizeFtAuthOperation(CREATE_LIBRARY_INVITATION)

        val res = txBuilder.createLibraryInvitationOperation(
            libraryId,
            devAccountBytes,
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
                echo("Invitation sent successfully, transaction rid: ${res.txRid.rid}")
            TransactionStatus.REJECTED ->
                throw PrintMessage("Failed to send invitation: ${res.status} -> ${res.rejectReason}")
        }
    } catch (e: Exception) {
        echo("Failed to create library invitation: ${e.message}", err = true)
        throw PrintMessage("Library invitation failed")
    }
}
