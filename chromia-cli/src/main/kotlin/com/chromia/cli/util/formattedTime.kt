package com.chromia.cli.util

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

fun getFormattedUtcDateTime(dateTime: Instant = Clock.systemUTC().instant()): String {
    val zonedDateTime = dateTime.atZone(ZoneId.of("UTC"))
    val pattern = DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH.mm.ss.nnnnnnnnn'Z'")
    val formattedDateTime = pattern.format(zonedDateTime)

    return formattedDateTime
}
