package com.chromia.cli.tools.launcher

import com.github.ajalt.clikt.completion.completionOption
import com.github.ajalt.clikt.core.NoOpCliktCommand
import mu.KotlinLogging
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.system.exitProcess

class CliLauncher(name: String): NoOpCliktCommand(name = name) {
    private val logger = KotlinLogging.logger {}
    init {
        completionOption()
    }

    override fun aliases() = createAliases()

    private fun translateExceptionToMessage(exception: Exception): String {
        val humanFriendlyMessage = when (exception) {
            is NullPointerException -> "A null value was encountered."
            is ArrayIndexOutOfBoundsException -> "The index provided is out of bounds."
            is IllegalArgumentException -> "An invalid argument was passed."
            is IllegalStateException -> "The state of the program is invalid."
            is UnsupportedOperationException -> "The operation is not supported."
            is FileNotFoundException -> "The file was not found."
            is IOException -> "An I/O error occurred."
            else -> "An error occurred."
        }
        val logFolder = System.getProperty("CHR_LOG_FOLDER") ?: "logs"
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
        } catch (e: Exception) {
            logger.error(e.message, e)
            val errorMessage = translateExceptionToMessage(e)
            echo(errorMessage, err = true)
            exitProcess(1)
        }
    }
}
