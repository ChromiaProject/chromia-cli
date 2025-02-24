package com.chromia.cli.sql

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.tools.formatter.info
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.Whitespace
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach

class SqlStatisticsRenderer(private val cliktCommand: CliktCommand) {

    fun display(allEntries: List<SqlStatisticsEntry>) {
        val groupedByTestCases = allEntries.groupBy { it.testCaseName }
                .mapValues { (_, entries) -> entries.sortedBy { it.event.startTimeMs } }

        groupedByTestCases.forEach { (testCaseName, entries) ->
            displayTable(testCaseName, entries)
        }
    }

    private fun displayTable(testCaseName: String?, entries: List<SqlStatisticsEntry>) {
        val totalQueries = entries.size
        val totalDuration = entries.sumOf { it.event.durationMs }
        val totalAffectedRows = entries.mapNotNull { it.event.rowCount }.sum()

        val statisticsTable = cliktCommand.defaultTable {

            captionTop(cliktCommand.info("SQL Stats: ${testCaseName ?: "System"}"), align = TextAlign.LEFT)

            align = TextAlign.LEFT
            overflowWrap = OverflowWrap.BREAK_WORD
            whitespace = Whitespace.PRE_LINE

            header {
                row("Query Type", "SQL", "Parameters", "Start Time", "Duration", "Rows Affected", "Status")
            }

            body {
                entries.forEach { entry ->
                    row(
                            getQueryTypeColor(entry.queryType)(entry.queryType.toString()),
                            entry.event.sql,
                            formatSqlParameters(entry.event.parameters),
                            formatInstant(entry.startTime),
                            "${entry.duration.inWholeMilliseconds}ms",
                            entry.event.rowCount?.toString() ?: "N/A",
                            formatQueryStatus(entry.event.error),
                    )
                }

                row {
                    cell("Total Queries: $totalQueries, Total Time: ${totalDuration}ms, Total Affected Rows: $totalAffectedRows") {
                        columnSpan = 7
                        align = TextAlign.RIGHT
                    }
                }
            }
        }

        cliktCommand.echo("\n\n")
        cliktCommand.echo(statisticsTable)
    }

    private fun formatSqlParameters(parameters: List<Any?>): String {
        return parameters.joinToString { it.toString() }
    }

    private fun formatQueryStatus(error: Exception?): String {
        return if (error == null) {
            TextColors.green("OK")
        } else {
            TextColors.red(error.message ?: "FAILED")
        }
    }

    private fun formatInstant(instant: java.time.Instant): String {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(
                instant.atZone(java.time.ZoneId.systemDefault())
        )
    }

    private fun getQueryTypeColor(queryType: SqlQueryType): TextColors {
        return if (queryType == SqlQueryType.SYSTEM) {
            TextColors.gray
        } else {
            TextColors.green
        }
    }
}
