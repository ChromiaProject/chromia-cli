package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.external.UPDATE_LIBRARY_VERSION_DESCRIPTION
import com.chromia.library.chain.versioning.external.updateLibraryDescriptionOperation
import com.chromia.library.chain.versioning.external.updateLibraryVersionDescriptionOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus

class UpdateLibraryDescription: AbstractLibraryCommand(
        name = "update-description",
        help = "Update description for library or description of specific library version"
) {
    override val hiddenFromHelp = true

    private val libraryId by argument(
            help = "ID of the library"
    )

    private val newDescription by option("--description", help = "New description").required()

    private val version by option(
            "--version",
            help = "Library version, if no version is defined it will update library description")

    override fun run() {
        authorizeFtAuthOperation(UPDATE_LIBRARY_VERSION_DESCRIPTION)
        val tx = if (version.isNullOrEmpty()) {
            txBuilder.updateLibraryDescriptionOperation(libraryId, newDescription)
        } else {
            txBuilder.updateLibraryVersionDescriptionOperation(libraryId, version!!, newDescription)
        }

        val res = tx.addNop().postAwaitConfirmation()

        when (res.status) {
            TransactionStatus.UNKNOWN ->
                echo("transaction with rid ${res.txRid.rid} was posted but has unknown status")

            TransactionStatus.WAITING ->
                echo("transaction with rid ${res.txRid.rid} was posted but is still pending")

            TransactionStatus.CONFIRMED -> {
                echo("Description successfully updated with transaction ${res.txRid}")
            }
            TransactionStatus.REJECTED ->
                throw PrintMessage(
                        "Failed to update description: ${res.rejectReason ?: "Unknown error"}",
                        statusCode = 1
                )
        }
    }
}