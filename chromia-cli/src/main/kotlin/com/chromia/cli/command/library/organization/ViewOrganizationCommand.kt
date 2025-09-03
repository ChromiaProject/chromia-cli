package com.chromia.cli.command.library.organization

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.library.chain.versioning.Organization
import com.chromia.library.chain.versioning.external.getOrganization
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.table.table

class ViewOrganizationCommand : AbstractLibraryCommand(
    name = "view",
    help = "View organization details",
    hideKeyPairSourceHelpMessage = true
) {

    private val organizationId by option(
        "--org-id",
        help = "ID of the organization to view"
    ).required()

    override fun run() {
        try {
            client.getOrganization(organizationId)?.let {
                displayOrganizationTable(it)
            } ?: echo("Organization not found", err = true)
        } catch (e: Exception) {
            echo("Failed to fetch organization details: ${e.message}", err = true)
            throw CliktError("Organization view failed")
        }
    }

    private fun displayOrganizationTable(org: Organization) {
        val table = table {
            header {
                style = bold + blue
                row("Property", "Value")
            }
            body {
                row("ID", org.id)
                row("Name", org.name)
                row("Description", org.description)
                row("Official", if (org.isOfficial) green("✓ Yes") else red("✗ No"))
                row("Created", org.createdAt)
            }
        }

        echo()
        echo(bold(cyan("Organization Details")))
        echo("═".repeat(50))
        echo(table)
        echo()
    }
}
