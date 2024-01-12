package com.chromia.cli.tools.formatter

import com.github.ajalt.mordant.table.TableBuilder
import com.google.gson.GsonBuilder
import com.chromia.cli.tools.mordant.CellContent
import com.chromia.cli.tools.mordant.TableBuilderInstance

fun jsonTable(init: TableBuilder.() -> Unit): String {
    val tableBuilder = TableBuilderInstance().apply(init)
    return if (tableBuilder.headerSection.rows.size > 0) {
        val headers: List<String> = tableBuilder.headerSection.rows[0].cells.map { (it.content as CellContent.TextContent).text }
        val rows: List<List<String>> = tableBuilder.bodySection.rows.map { row ->
            row.cells.map { cell ->
                (cell.content as CellContent.TextContent).text
            }
        }
        jsonTable(headers, rows)
    } else {
        json(tableBuilder.bodySection.rows.associate { row ->
            fixKey((row.cells[0].content as CellContent.TextContent).text) to
                    (row.cells[1].content as CellContent.TextContent).text
        })
    }
}

fun jsonTable(headers: List<String>, rows: List<List<String>>): String = json(
        rows.map { row ->
            row.withIndex().associate { cell ->
                fixKey(headers[cell.index]) to cell.value
            }
        })

private fun fixKey(key: String) = key.replace(' ', '_').replace(":", "")

private fun json(data: Any) = GsonBuilder().serializeNulls().setPrettyPrinting().create().toJson(data)
