package com.chromia.cli.command

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.cli.tools.config.ConfigurationFormat
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.config.configurationFormatOption
import com.chromia.cli.util.blockchainOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.mordant.rendering.TextColors
import net.postchain.rell.api.base.RellCliEnv

class BuildCommand : ChromiaCommand(help = "Build an application and create a blockchain configuration") {
    override val invokeWithoutSubcommand: Boolean
        get() = true

    private val blockchain by blockchainOption("Explicitly specify which blockchain(s) to compile", "BLOCKCHAIN").multiple()
    private val settings by chromiaModelOption()
    private val format by configurationFormatOption()
    private val hideLibWarnings by option("--hide-lib-warnings")
            .flag(default = false)
            .help("Hide library warnings in build output")

    override fun run() {
        ChromiaCompileApi.build(
                BuildCommandCliEnv(this, hideLibWarnings),
                settings.model.filterBlockchains(blockchain)
        ).forEach {
                    when (format) {
                        ConfigurationFormat.GTV -> it.saveAsBinaryGtv(settings.targetDir.toPath())

                        ConfigurationFormat.XML -> try {
                            it.save(settings.targetDir.toPath())
                        } catch (e: IllegalArgumentException) {
                            throw CliktError("Blockchain ${it.name} contains ${e.message} Use --format=GTV")
                        }
                    }
                }
    }
}

class BuildCommandCliEnv(
        val command: CliktCommand,
        private val hideLibWarnings: Boolean = false
) : RellCliEnv {
    var userWarningCount = 0
    private val summaryPattern = Regex("Errors: (\\d+) Warnings: (\\d+)")
    private val libraryWarningPattern = Regex("^lib/.*Warning:")
    private val userWarningPattern = Regex("^(?!lib/).*Warning:")

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

