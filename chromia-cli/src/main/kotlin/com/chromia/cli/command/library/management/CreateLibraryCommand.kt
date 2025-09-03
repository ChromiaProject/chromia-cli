package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.BlockchainModel
import com.chromia.library.chain.versioning.external.CREATE_LIBRARY
import com.chromia.library.chain.versioning.external.createLibraryOperation
import com.chromia.library.chain.versioning.external.getLibraryByOrganizationAndName
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.prompt
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus
import java.io.File
import kotlin.io.path.div

class CreateLibraryCommand : AbstractLibraryCommand(
    name = "create",
    help = "Create a new library in an organization"
) {
    override val hiddenFromHelp = true

    private val displayName by option(
        "--name",
        "-n",
        help = "Display name for the library"
    ).prompt("Library name")

    private val description by option(
        "--description",
        "-d",
        help = "A brief description of the library."
    ).prompt("Library description")

    private val organizationId by option(
        "--organization",
        "-o",
        help = "Organization ID that will own this library"
    ).required()

    private val version by option(
        "--version",
        "-v",
        help = "Initial version of the library"
    ).default("0.0.1")

    private val library by option(
        "--library",
        help = "Library to deploy"
    ).required()

    override fun run() {
        authorizeFtAuthOperation(CREATE_LIBRARY)

        val sourceDir = settings.model
            ?.compile
            ?.source
            ?: throw PrintMessage("No source directory found in chromia.yml file", statusCode = 1)

        val module = settings.model
            ?.blockchains[library]
            ?.takeIf { it.type == BlockchainModel.Type.LIBRARY }
            ?.module
            ?.replace(".", File.separator)
            ?: throw PrintMessage("Library not found: $library in chromia.yml file", statusCode = 1)

        val libPath = sourceDir / module

        val files = collectFiles(libPath)

        if (files.isEmpty()) {
            throw PrintMessage("No source files found")
        }

        echo("Found ${files.size} files to include in library")

        validateLibraryCode(library)

        val rid = calculateRid(libPath)
        val res = txBuilder.createLibraryOperation(
            displayName = displayName,
            description = description,
            files = files,
            organizationId = organizationId,
            version = version,
            rid = rid
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
                val library = client.getLibraryByOrganizationAndName(organizationId, displayName)
                val libraryId = library?.id ?: "Unknown"
                echo("Library created successfully in transaction ${res.txRid} with id: $libraryId")
            }
            TransactionStatus.REJECTED ->
                throw PrintMessage(
                    "Unable to create library: ${res.status}[${res.httpStatusCode}] --> ${res.rejectReason}",
                    statusCode = 1
                )
        }
    }
}
