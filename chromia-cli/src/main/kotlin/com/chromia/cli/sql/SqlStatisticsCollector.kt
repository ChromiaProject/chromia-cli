package com.chromia.cli.sql

import net.postchain.rell.api.gtx.SqlExecutionEvent
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult


class SqlStatisticsCollector {
    private val sqlStatistics = mutableListOf<SqlStatisticsEntry>()

    private var currentTestCaseName: String? = null

    fun onSqlExecutionFinished(event: SqlExecutionEvent) {
        sqlStatistics.add(SqlStatisticsEntry(event, currentTestCaseName))
    }

    fun getStatistics(): List<SqlStatisticsEntry> = sqlStatistics

    fun onTestCaseStart(case: UnitTestCase) {
        currentTestCaseName = case.name
    }

    fun onTestCaseFinished(result: UnitTestCaseResult) {
        currentTestCaseName = null
    }
}

