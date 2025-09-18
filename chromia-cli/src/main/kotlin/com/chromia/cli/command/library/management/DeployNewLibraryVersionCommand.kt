package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.model.BlockchainModel
import com.chromia.library.chain.versioning.external.CREATE_LIBRARY_VERSION
import com.chromia.library.chain.versioning.external.createLibraryVersionOperation
import com.chromia.library.chain.versioning.external.getLatestLibraryVersion
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.tx.TransactionStatus
import java.io.File

class DeployNewLibraryVersionCommand : AbstractLibraryCommand(
    name = "deploy",
    help = "Deploy a new version of a library"
) {

    override val hiddenFromHelp = true

    private val libraryId by option(
        "--id",
        help = "ID of the library to deploy a new version for"
    ).required()

    private val version by option(
        "--version",
        "-v",
        help = "Version number for this deployment"
    ).required()

    private val description by option(
        "--description",
        "-d",
        help = "A brief description of the library."
    ).required()

    private val library by option(
        "--library",
        help = "Library to deploy"
    ).required()

    private val verifyRid by option(
        "--verify-rid",
        help = "Verify the RID of the library code"
    ).flag(default = false)

    override fun run() {
        authorizeFtAuthOperation(CREATE_LIBRARY_VERSION)

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

        val libPath = sourceDir.resolve(module)

        val files = collectFiles(libPath)

        if (files.isEmpty()) {
            throw PrintMessage("No source files found")
        }

        echo("Found ${files.size} files to include in library")

        validateLibraryCode(library)
        val rid = calculateRid(libPath)

        if (verifyRid) {
            verifyIfCodeHasActuallyChanged(libraryId, rid)
        }

        val res = txBuilder.createLibraryVersionOperation(
            id = libraryId,
            description = description,
            files = files,
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

            TransactionStatus.CONFIRMED ->
                echo(
                    "New library version deployed successfully for library '$libraryId' with version '$version' and RID ${res.txRid.rid}"
                )
            TransactionStatus.REJECTED ->
                throw PrintMessage(
                    "Unable to create library: ${res.status}[${res.httpStatusCode}] --> ${res.rejectReason}",
                    statusCode = 1
                )
        }
    }

    private fun verifyIfCodeHasActuallyChanged(
        libraryId: String,
        newRid: ByteArray,
    ) {
        client.getLatestLibraryVersion(libraryId)
            ?.rid
            ?.let {
                require(!newRid.contentEquals(it.data)) {
                    "New version of library '$libraryId' has the same code as the latest version. Aborting..."
                }
            }
    }
}
