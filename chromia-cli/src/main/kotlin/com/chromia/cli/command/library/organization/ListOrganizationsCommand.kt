package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.library.chain.versioning.TypesSOrganization
import com.chromia.library.chain.versioning.external.getOrganizations
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.long

class ListOrganizationsCommand : AbstractLibraryCommand(
    name = "list",
    help = "List all organizations",
    hideKeyPairSourceHelpMessage = true
) {

    private val limit by option(
        "--limit",
        "-l",
        help = "Maximum number of organizations to display"
    ).long().default(Long.MAX_VALUE)

    // TODO: Pagination
    private val offset by option(
        "--offset",
        "-o",
        help = "Number of organizations to skip"
    ).long().default(0)

    override fun run() {
        try {
            val organizations = client.getOrganizations(limit, offset)

            if (organizations.isEmpty()) {
                echo("No organizations found")
            } else {
                displayOrganizationsTable(organizations)
            }
        } catch (e: Exception) {
            echo("Failed to fetch organizations: ${e.message}", err = true)
            throw PrintMessage("Organization listing failed")
        }
    }

    private fun displayOrganizationsTable(organizations: List<TypesSOrganization>) {
        echo("Available Organizations:")
        echo(
            defaultTable {
                header {
                    row("ID", "Name", "Description", "Official", "Created")
                }
                body {
                    organizations.forEach { org ->
                        val isOfficial = if (org.isOfficial) "Yes" else "No"
                        row(
                            org.id,
                            org.name,
                            org.description,
                            isOfficial,
                            org.createdAt
                        )
                    }
                }
            }
        )
        echo("Total: ${organizations.size} organizations")
    }
}
