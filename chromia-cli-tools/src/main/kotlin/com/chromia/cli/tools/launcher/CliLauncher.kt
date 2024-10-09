package com.chromia.cli.tools.launcher

import com.chromia.build.tools.compile.ValidationException
import com.chromia.cli.tools.formatter.PanelHelpFormatter
import com.chromia.cli.tools.formatter.chromiaTheme
import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.AnsiLevel
import com.github.ajalt.mordant.rendering.Theme
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalDetection
import mu.KotlinLogging
import net.postchain.client.exception.ClientError
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.base.RellCliExitException
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.sql.SQLException
import kotlin.system.exitProcess

open class CliLauncher(name: String) : NoOpCliktCommand(name = name) {
    private val logger = KotlinLogging.logger {}

    init {
        completionOption()
        val detectedTerminalInfo = TerminalDetection.detectTerminal(null, null, null, null, detectedStdinInteractive = false, detectedStdoutInteractive = false)
        context {
            terminal = Terminal(theme = when (detectedTerminalInfo.ansiLevel) {
                AnsiLevel.NONE -> Theme.Plain
                AnsiLevel.ANSI16 -> Theme.Plain
                else -> chromiaTheme
            }
            )
            helpFormatter = { PanelHelpFormatter(it) }
        }
    }

    override fun aliases() = createAliases() +
            mapOf("generate-client-stubs" to listOf("generate", "client-stubs")) // Deprecated alias

    private fun translateExceptionToMessage(exception: Exception): String {
        val humanFriendlyMessage = when (exception) {
            is NullPointerException -> "A null value was encountered."
            is ArrayIndexOutOfBoundsException -> "The index provided is out of bounds."
            is IllegalArgumentException -> "An invalid argument was passed."
            is IllegalStateException -> "The state of the program is invalid."
            is UnsupportedOperationException -> "The operation is not supported."
            is FileNotFoundException -> "The file was not found."
            is IOException -> "An I/O error occurred."
            is ValidationException -> "Invalid blockchain configuration."
            else -> "An error occurred."
        }

        val logFolder = System.getProperty("CHR_LOG_FOLDER") ?: "/usr/app/logs"
        val suffix = "Please refer to log file for more details: ${logFolder}${File.separator}chromia-cli.log"
        return "$humanFriendlyMessage ${formatExceptionMessage(exception)}$suffix"
    }

    private fun formatExceptionMessage(exception: Exception): String {
        val message = exception.message
        return if (message.isNullOrBlank()) {
            ""
        } else {
            "$message. "
        }
    }

    fun catchingAllExceptionsMain(args: Array<out String>) {
        try {
            main(args.asList())
        } catch (e: RellCliExitException) {
            exitProcess(1)
        } catch (e: ClientError) {
            echo(e.message, err = true)
            exitProcess(1)
        } catch (e: RellCliException) {
            echo(e.message, err = true)
            exitProcess(2)
        } catch (e: SQLException) {
            echo("Error connecting to database: ${e.message}", err = true)
            echo("Check your database connection")
            exitProcess(2)
        } catch (e: Exception) {
            logger.error(e.message, e)
            val errorMessage = translateExceptionToMessage(e)
            echo(errorMessage, err = true)
            exitProcess(3)
        }
    }
}
