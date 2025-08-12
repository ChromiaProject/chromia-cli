package com.chromia.cli.util

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.mordant.rendering.TextColors
import net.postchain.rell.api.base.RellCliEnv

class BuildCliEnv(
        val command: CliktCommand,
        private val hideLibWarnings: Boolean = false
) : RellCliEnv {
    var userWarningCount = 0
    private val summaryPattern = Regex("Errors: (\\d+) Warnings: (\\d+)")
    private val libraryWarningPattern = Regex("^lib/.*Warning:")
    private val userWarningPattern = Regex("^(?!lib/).*Warning:")
    private var seenError = false
    val hasError: Boolean
        get() = seenError

    override fun error(msg: String) {
        when {
            libraryWarningPattern.containsMatchIn(msg) -> if (!hideLibWarnings) command.echo(msg, err = true)
            userWarningPattern.containsMatchIn(msg) -> {
                command.echo(msg, err = true)
                userWarningCount++
            }
            summaryPattern.find(msg) != null -> processSummary(msg)
            else -> command.echo(msg, err = true)
        }
    }

    override fun print(msg: String) = command.echo(msg)

    private fun formatCountMsg(label: String, count: Int, warningColor: TextColors): String {
        val color = if (count > 0) warningColor else TextColors.brightGreen
        return color("$label: $count")
    }
    
    private fun processSummary(msg: String) {
        summaryPattern.find(msg)?.let { matchResult ->
            val (errorCount, warningCount) = matchResult.destructured
            val totalWarnings = warningCount.toInt()
            val libWarnings = totalWarnings - userWarningCount

            seenError = errorCount.toInt() > 0

            val summary = buildString {
                append(formatCountMsg("Errors", errorCount.toInt(), TextColors.brightRed))
                append(", ")
                append(formatCountMsg("User Warnings", userWarningCount, TextColors.brightYellow))
                append(", ")
                append(formatCountMsg("Lib Warnings", libWarnings, TextColors.brightYellow))
            }
            command.echo(summary, err = true)
        }
    }
}

