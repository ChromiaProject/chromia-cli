package com.chromia.cli.command.library.management

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.library.chain.versioning.TypesESortByCreatedAt
import com.chromia.library.chain.versioning.TypesSLibrary
import com.chromia.library.chain.versioning.external.getLibraries
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.ColumnWidth

class ListLibrariesCommand : AbstractLibraryCommand(
    name = "list",
    help = "List all available libraries",
    hideKeyPairSourceHelpMessage = true
) {

    private val limit by option(
        "--limit",
        "-l",
        help = "Maximum number of libraries to display"
    ).long().default(20)

    private val offset by option(
        "--offset",
        "-o",
        help = "Number of libraries to skip"
    ).long()
        .default(0)

    private val sortBy by option(
        "--sort-by",
        help = "Sort order for libraries"
    ).choice(
        "asc" to TypesESortByCreatedAt.ASC,
        "desc" to TypesESortByCreatedAt.DESC
    ).default(TypesESortByCreatedAt.DESC)

    override fun run() = try {
        val libs = client.getLibraries(sortBy, limit, offset)

        if (libs.isEmpty()) {
            echo("No libraries found")
        } else {
            displayLibrariesTable(libs)
        }
    } catch (e: Exception) {
        echo("Failed to fetch libraries: ${e.message}")
        throw PrintMessage("Library listing failed")
    }

    private fun displayLibrariesTable(libs: List<TypesSLibrary>) {
        echo("Available Libraries:")
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
                    libs.forEach { lib ->
                        val isOfficial = if (lib.isOfficial) "Yes" else "No"
                        row(
                            lib.id,
                            lib.displayName,
                            lib.organizationName,
                            lib.latestVersion,
                            isOfficial,
                            lib.description
                        )
                    }
                }
            }
        )
        echo("Total: ${libs.size} libraries")
    }
}
