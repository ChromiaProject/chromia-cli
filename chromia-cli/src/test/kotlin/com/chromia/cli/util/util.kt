package com.chromia.cli.util

import org.apache.logging.log4j.core.LogEvent
import org.apache.logging.log4j.core.Logger
import org.apache.logging.log4j.core.LoggerContext
import org.apache.logging.log4j.core.test.appender.ListAppender

fun captureLog4jLoggerOutput(loggerClass: Class<*>, action: () -> Unit): List<String> {
    val loggerContext = LoggerContext.getContext(false)
    val logger = loggerContext.getLogger(loggerClass) as Logger
    val appender = ListAppender("TestListAppender")
    loggerContext.configuration.addLoggerAppender(logger, appender)
    try {
        appender.start()
        action()
    } finally {
        appender.stop()
    }

    return appender.events.map { event: LogEvent -> event.message.formattedMessage }
}