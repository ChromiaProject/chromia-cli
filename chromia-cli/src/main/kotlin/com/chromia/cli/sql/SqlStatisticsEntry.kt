package com.chromia.cli.sql

import net.postchain.rell.api.gtx.SqlExecutionEvent
import java.time.Instant
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class SqlStatisticsEntry(
        val event: SqlExecutionEvent,
        val testCaseName: String?,
) {
    val queryType: SqlQueryType = if (event.isSystem) SqlQueryType.SYSTEM else SqlQueryType.USER
    val duration get() = event.durationMs.toDuration(DurationUnit.MILLISECONDS)
    val startTime get() = Instant.ofEpochMilli(event.startTimeMs)
}
