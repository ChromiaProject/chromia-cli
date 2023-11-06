package com.chromia.cli.util


import org.apache.logging.log4j.Level
import org.apache.logging.log4j.core.Filter.Result
import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.filter.AbstractFilter


class ConfigureLog4j {
    fun configureLogLevel(logLevel: String) {
        val context = LoggerContext.getContext(false)
        val config = context.configuration
        val postchainConfig = config.getLoggerConfig("net.postchain")

        val level = Level.toLevel(logLevel, Level.INFO)
        postchainConfig.level = level
        if (logLevel == "quite") {
            postchainConfig.addFilter(Log4jFilter)
        }
        context.updateLoggers()
    }

    companion object Log4jFilter : AbstractFilter() {
        override fun filter(event: LogEvent): Result {
            val loggerName = event.loggerName
            if (loggerName.contains("BaseBlockchainProcessManager") || loggerName.contains("RestApi")) {
                return Result.ACCEPT
            }
            return Result.DENY
        }
    }
}
