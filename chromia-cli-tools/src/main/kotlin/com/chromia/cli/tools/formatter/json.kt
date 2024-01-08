package com.chromia.cli.tools.formatter

import com.github.ajalt.mordant.table.TableBuilder
import com.google.gson.GsonBuilder
import com.chromia.cli.tools.mordant.CellContent
import com.chromia.cli.tools.mordant.TableBuilderInstance

fun jsonTable(init: TableBuilder.() -> Unit): String {
    val tableBuilder = TableBuilderInstance().apply(init)
    val table: List<Map<String, String>> = tableBuilder.bodySection.rows.withIndex().map { row ->
        row.value.cells.withIndex().associate { cell ->
            (tableBuilder.headerSection.rows[0].cells[cell.index].content as CellContent.TextContent).text to
                    (cell.value.content as CellContent.TextContent).text
        }
    }
    return GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(table)
}
