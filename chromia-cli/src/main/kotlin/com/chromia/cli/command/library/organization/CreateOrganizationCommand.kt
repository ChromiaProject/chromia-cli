package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.CREATE_ORGANIZATION
import com.chromia.library.chain.versioning.external.createOrganizationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus

class CreateOrganizationCommand : AbstractLibraryCommand(
    name = "create",
    help = "Create a new organization"
) {

    private val organizationId by option(
        "--org-id",
        help = "Unique identifier for the organization"
    ).required()

    private val name by option(
        "--name",
        "-n",
        help = "Name of the organization"
    ).required()

    private val description by option(
        "--description",
        "-d",
        help = "Description of the organization"
    ).required()

    private val isOfficial by option(
        "--is-official",
        help = "Is official Chromia library"
    ).flag(default = false)

    override fun run() {
        try {
            authorizeFtAuthOperation(CREATE_ORGANIZATION)

            val res = txBuilder.createOrganizationOperation(
                id = organizationId,
                name = name,
                description = description,
                isOfficial = isOfficial
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
                    echo(
                        "Organization '$name' created successfully with ID: $organizationId, with transaction ${res.txRid.rid}"
                    )
                TransactionStatus.REJECTED ->
                    throw PrintMessage(
                        "Failed to create organization: ${res.rejectReason}",
                        statusCode = 1
                    )
            }
        } catch (e: Exception) {
            echo("Failed to create organization: ${e.message}", err = true)
            throw PrintMessage("Organization creation failed")
        }
    }
}
