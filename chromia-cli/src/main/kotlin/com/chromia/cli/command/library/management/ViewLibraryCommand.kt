package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.library.chain.versioning.TypesSLibrary
import com.chromia.library.chain.versioning.external.getLibrary
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.ColumnWidth

class ViewLibraryCommand : AbstractLibraryCommand(
    name = "view",
    help = "View detailed information about a library",
    hideKeyPairSourceHelpMessage = true
) {

    private val libraryId by argument(
        name = "LIBRARY_ID",
        help = "ID of the library to view"
    )

    override fun run() {
        try {
            client.getLibrary(libraryId)?.let {
                displayLibrary(it)
            } ?: echo("Library not found", err = true)
        } catch (e: Exception) {
            echo("Failed to fetch library details: ${e.message}")
            throw PrintMessage("Library view failed")
        }
    }

    private fun displayLibrary(library: TypesSLibrary) {
        echo(
            defaultTable {
                column(0) { width = ColumnWidth.Auto }
                column(1) { width = ColumnWidth.Auto }
                column(2) { width = ColumnWidth.Auto }
                column(3) { width = ColumnWidth.Auto }
                column(4) { width = ColumnWidth.Auto }
                column(5) { width = ColumnWidth.Expand() }

                align = TextAlign.LEFT
                overflowWrap = OverflowWrap.BREAK_WORD
                whitespace = Whitespace.PRE_LINE

                header {
                    row("ID", "Name", "Organization", "Version", "Official", "Description")
                }
                body {
                    val isOfficial = if (library.isOfficial) "Yes" else "No"
                    row(
                        library.id,
                        library.displayName,
                        library.organizationName,
                        library.latestVersion,
                        isOfficial,
                        library.description
                    )
                }
            }
        )
    }
}
