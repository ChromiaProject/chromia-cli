package com.chromia.cli.sql

import com.chromia.cli.command.SqlLoggingType
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.tools.formatter.info
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.OverflowWrap
import com.github.ajalt.mordant.rendering.TextAlign
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.rendering.Whitespace
import java.time.format.DateTimeFormatter
import kotlin.collections.forEach

private data class SqlStats(val queryCount: Int, val duration: Long, val rowCount: Int)

class SqlStatisticsRenderer(
    private val cliktCommand: CliktCommand,
    private val sqlLogType: SqlLoggingType = SqlLoggingType.BOTH
) {

    fun display(allEntries: List<SqlStatisticsEntry>) {
        val groupedByTestCases = allEntries.groupBy { it.testCaseName }
                .mapValues { (_, entries) -> entries.sortedBy { it.event.startTimeMs } }

        groupedByTestCases.forEach { (testCaseName, entries) ->
            displayTable(testCaseName, entries)
        }
    }

    private fun displayTable(testCaseName: String?, entries: List<SqlStatisticsEntry>) {
        val (userQueries, systemQueries) = entries.partition { it.queryType == SqlQueryType.USER}

        val (userCount, userDuration, userRows) = userQueries.stats()
        val (systemCount, systemDuration, systemRows) = systemQueries.stats()
        
        val totalQueries = userCount + systemCount
        val totalDuration = userDuration + systemDuration
        val totalAffectedRows = userRows + systemRows

        val statisticsTable = cliktCommand.defaultTable {

            captionTop(cliktCommand.info("SQL Stats: ${testCaseName ?: "System"}"), align = TextAlign.LEFT)

            align = TextAlign.LEFT
            overflowWrap = OverflowWrap.BREAK_WORD
            whitespace = Whitespace.PRE_LINE

            header {
                row("Query Type", "SQL", "Parameters", "Start Time", "Duration", "Rows Affected", "Status")
            }

            body {
                entries.filter(::shouldLogEntry).forEach { entry ->
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
            }

            footer {
                row {
                    cell(buildString {
                        append(TextStyles.bold("Total Queries: ") + TextColors.yellow("$totalQueries")).append(", ")
                        append(TextStyles.bold("Total Time: ") + TextColors.yellow("${totalDuration}ms")).append(", ")
                        append(TextStyles.bold("Total Affected Rows: ") + TextColors.yellow("$totalAffectedRows"))
                    }) {
                        columnSpan = 7
                        align = TextAlign.LEFT
                    }
                }
                
                row {
                    cell(buildString {
                        append(TextStyles.bold("User Queries: ") + getQueryTypeColor(SqlQueryType.USER)("$userCount")).append(", ")
                        append(TextStyles.bold("User Time: ") + getQueryTypeColor(SqlQueryType.USER)("${userDuration}ms")).append(", ")
                        append(TextStyles.bold("User Affected Rows: ") + getQueryTypeColor(SqlQueryType.USER)("$userRows"))
                    }) {
                        columnSpan = 7
                        align = TextAlign.LEFT
                    }
                }

                row {
                    cell(buildString {
                        append(TextStyles.bold("System Queries: ") + getQueryTypeColor(SqlQueryType.SYSTEM)("$systemCount")).append(", ")
                        append(TextStyles.bold("System Time: ") + getQueryTypeColor(SqlQueryType.SYSTEM)("${systemDuration}ms")).append(", ")
                        append(TextStyles.bold("System Affected Rows: ") + getQueryTypeColor(SqlQueryType.SYSTEM)("$systemRows"))
                    }) {
                        columnSpan = 7
                        align = TextAlign.LEFT
                    }
                }
            }
        }

        cliktCommand.echo("\n\n")
        cliktCommand.echo(statisticsTable)
    }

    private fun shouldLogEntry(entry: SqlStatisticsEntry) = when(sqlLogType) {
        SqlLoggingType.USER -> !entry.event.isSystem
        SqlLoggingType.SYSTEM -> entry.event.isSystem
        SqlLoggingType.BOTH -> true
        SqlLoggingType.NONE -> false
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

    private fun List<SqlStatisticsEntry>.stats() =
        SqlStats(
            queryCount = size,
            duration = sumOf { it.event.durationMs },
            rowCount = sumOf { it.event.rowCount ?: 0 }
        )
}
