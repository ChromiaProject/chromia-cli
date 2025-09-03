package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.DELETE_DEVELOPER_FROM_ORGANIZATION
import com.chromia.library.chain.versioning.external.deleteDeveloperFromOrganizationOperation
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus

class RemoveUserCommand : AbstractLibraryCommand(
    name = "remove-user",
    help = "Remove a user from an organization"
) {
    override val hiddenFromHelp = true

    private val organizationId by argument(
        help = "ID of the organization to remove user from"
    )

    private val developerAccountId by argument(
        help = "Account ID of the developer to remove (hex string)"
    )

    private val force by option(
        "--force",
        "-f",
        help = "Skip confirmation prompt"
    ).flag(default = false)

    override fun run() {
        try {
            val accountIdBytes = developerAccountId.hexStringToByteArray()
            if (!force) {
                if (
                    YesNoPrompt(
                        "Are you sure you want to remove developer from organization $organizationId ?",
                        terminal,
                        default = false
                    ).ask() != true
                ) {
                    throw PrintMessage("Remove operation cancelled.")
                }
            }

            authorizeFtAuthOperation(DELETE_DEVELOPER_FROM_ORGANIZATION)

            val res = txBuilder.deleteDeveloperFromOrganizationOperation(
                organizationId = organizationId,
                accountId = accountIdBytes
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
                    echo("Developer removed from organization successfully!")
                    echo("Organization: $organizationId")
                    echo("Developer Account ID: $developerAccountId")
                }

                TransactionStatus.REJECTED ->
                    throw PrintMessage("Remove operation failed: ${res.status} -> ${res.rejectReason}")
            }
        } catch (e: Exception) {
            echo("Failed to remove developer from organization: ${e.message}")
            throw CliktError("Developer removal failed")
        }
    }
}
