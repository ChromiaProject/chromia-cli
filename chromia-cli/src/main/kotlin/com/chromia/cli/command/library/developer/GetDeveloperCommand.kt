package com.chromia.cli.command.library.developer

import com.chromia.cli.command.library.AbstractLibraryCommand
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.tools.util.convertMillisToLocalDateTime
import com.chromia.library.chain.users.Developer
import com.chromia.library.chain.users.external.getDeveloper
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.Whitespace
import com.github.ajalt.mordant.table.ColumnWidth
import net.postchain.common.hexStringToByteArray

class GetDeveloperCommand : AbstractLibraryCommand(
    "get",
    help = "Get a library developer",
    hideKeyPairSourceHelpMessage = true
) {

    private val accountId by argument(
        help = "Developer account ID"
    )

    override fun run() {
        client.getDeveloper(accountId.hexStringToByteArray())?.let { developer ->
            displayDevInfo(developer)
        } ?: echo("No developer found with the given account id")
    }

    private fun displayDevInfo(developer: Developer) {
        echo(
            defaultTable {
                column(0) { width = ColumnWidth.Auto }
                column(1) { width = ColumnWidth.Auto }
                column(2) { width = ColumnWidth.Auto }
                column(3) { width = ColumnWidth.Auto }

                align = TextAlign.LEFT
                overflowWrap = OverflowWrap.BREAK_WORD
                whitespace = Whitespace.PRE_LINE

                header {
                    row("Account", "Name", "Created at", "Updated at")
                }
                body {
                    row(
                        developer.account,
                        developer.name,
                        developer.createdAt.convertMillisToLocalDateTime(),
                        developer.updatedAt.convertMillisToLocalDateTime(),
                    )
                }
            }
        )
    }
}
