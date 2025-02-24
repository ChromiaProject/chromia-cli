package com.chromia.cli.sql

import assertk.assertThat
import assertk.assertions.*
import net.postchain.rell.api.gtx.SqlExecutionEvent
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult
import net.postchain.rell.base.utils.UnitTestResult
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SqlStatisticsCollectorTest {
    private lateinit var collector: SqlStatisticsCollector

    @BeforeEach
    fun setup() {
        collector = SqlStatisticsCollector()
    }

    @Test
    fun `should collect SQL execution statistics`() {
        val event = SqlExecutionEvent(
                sql = "SELECT * FROM users",
                parameters = listOf("param1", "param2"),
                startTimeMs = 1000L,
                durationMs = 100L,
                rowCount = 5,
                isSystem = false,
                error = null
        )

        collector.onSqlExecutionFinished(event)

        val stats = collector.getStatistics()
        assertThat(stats).hasSize(1)
        assertThat(stats[0].event).isEqualTo(event)
        assertThat(stats[0].testCaseName).isNull()
    }

    @Test
    fun `should track test case context`() {
        val testCase = mock<UnitTestCase> {
            on { name } doReturn "test_case_1"
        }

        val event = SqlExecutionEvent(
                sql = "SELECT * FROM test_table",
                parameters = listOf(),
                startTimeMs = 2000L,
                durationMs = 50L,
                rowCount = 1,
                isSystem = true,
                error = null
        )

        collector.onTestCaseStart(testCase)
        collector.onSqlExecutionFinished(event)
        collector.onTestCaseFinished(UnitTestCaseResult(testCase, mock<UnitTestResult>()))

        val stats = collector.getStatistics()
        assertThat(stats).hasSize(1)
        assertThat(stats[0].event).isEqualTo(event)
        assertThat(stats[0].testCaseName).isEqualTo("test_case_1")
    }

    @Test
    fun `should handle multiple SQL executions`() {
        val event1 = SqlExecutionEvent(
                sql = "INSERT INTO table1",
                parameters = listOf(),
                startTimeMs = 1000L,
                durationMs = 100L,
                rowCount = 1,
                isSystem = false,
                error = null
        )
        val event2 = SqlExecutionEvent(
                sql = "UPDATE table1",
                parameters = listOf(),
                startTimeMs = 1200L,
                durationMs = 150L,
                rowCount = 2,
                isSystem = false,
                error = null
        )

        collector.onSqlExecutionFinished(event1)
        collector.onSqlExecutionFinished(event2)

        val stats = collector.getStatistics()
        assertThat(stats).hasSize(2)
        assertThat(stats[0].event).isEqualTo(event1)
        assertThat(stats[1].event).isEqualTo(event2)
    }

    @Test
    fun `should clear test case context after test completion`() {
        val testCase = mock<UnitTestCase> {
            on { name } doReturn "test_case_1"
        }
        val event1 = SqlExecutionEvent(
                sql = "SELECT 1",
                parameters = listOf(),
                startTimeMs = 1000L,
                durationMs = 50L,
                rowCount = null,
                isSystem = true,
                error = null
        )
        val event2 = SqlExecutionEvent(
                sql = "SELECT 2",
                parameters = listOf(),
                startTimeMs = 2000L,
                durationMs = 50L,
                rowCount = null,
                isSystem = true,
                error = null
        )

        collector.onTestCaseStart(testCase)
        collector.onSqlExecutionFinished(event1)
        collector.onTestCaseFinished(UnitTestCaseResult(testCase, mock<UnitTestResult>()))
        collector.onSqlExecutionFinished(event2)

        val stats = collector.getStatistics()
        assertThat(stats).hasSize(2)
        assertThat(stats[0].testCaseName).isEqualTo("test_case_1")
        assertThat(stats[1].testCaseName).isNull()
    }
}
