package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.library.chain.versioning.TypesSLibraryVersion
import com.chromia.library.chain.versioning.external.getLibraryVersions
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.ColumnWidth

class ListVersionsCommand : AbstractLibraryCommand(
    name = "versions",
    help = "List all versions of a library",
    hideKeyPairSourceHelpMessage = true
) {

    private val libraryId by argument(
        help = "ID of the library to list versions for"
    )

    private val limit by option(
        "--limit",
        "-l",
        help = "Maximum number of versions to display"
    ).long().default(10L)

    private val offset by option(
        "--offset",
        "-o",
        help = "Number of versions to skip"
    ).long().default(0)

    override fun run() {
        try {
            val versions = client.getLibraryVersions(libraryId, limit, offset)

            if (versions.isEmpty()) {
                echo("No versions found for library '$libraryId'")
            } else {
                displayVersionsTable(versions)
            }
        } catch (e: Exception) {
            echo("Failed to fetch library versions: ${e.message}", err = true)
            throw PrintMessage("Library versions listing failed")
        }
    }

    private fun displayVersionsTable(versions: List<TypesSLibraryVersion>) {
        echo("Available Versions:")
        echo(
            defaultTable {
                column(0) { width = ColumnWidth.Auto }
                column(1) { width = ColumnWidth.Expand() }

                align = TextAlign.LEFT
                overflowWrap = OverflowWrap.BREAK_WORD
                whitespace = Whitespace.PRE_LINE
                header {
                    row("Version", "Description")
                }
                body {
                    versions.forEach { version ->
                        row(
                            version.version,
                            version.versionDescription,
                        )
                    }
                }
            }
        )
        echo("Total: ${versions.size} versions")
    }
}
