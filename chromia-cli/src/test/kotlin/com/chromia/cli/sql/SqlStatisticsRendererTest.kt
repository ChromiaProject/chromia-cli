package com.chromia.cli.sql

import assertk.assertThat
import assertk.assertions.*
import com.chromia.cli.command.ChromiaCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import net.postchain.rell.api.gtx.SqlExecutionEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DummyCommand : ChromiaCommand(help = "Dummy command used for unit testing") {
    override fun run() {}
}

class SqlStatisticsRendererTest {
    private lateinit var command: DummyCommand
    private lateinit var renderer: SqlStatisticsRenderer
    private lateinit var logger: TerminalRecorder
    private lateinit var testTerminal: Terminal

    @BeforeEach
    fun setup() {
        logger = TerminalRecorder(width = 1000, outputInteractive = true)
        testTerminal = Terminal(terminalInterface = logger)
        command = DummyCommand().context { terminal = testTerminal }.also { it.parse(listOf()) }
        renderer = SqlStatisticsRenderer(command)
    }

    @Test
    fun `should display SQL statistics grouped by test cases`() {
        val testCase1Event = createSqlEvent(
                sql = "SELECT * FROM users",
                startTimeMs = 1000L,
                durationMs = 100L,
                rowCount = 5,
                isSystem = false
        )
        val testCase2Event = createSqlEvent(
                sql = "INSERT INTO logs",
                startTimeMs = 2000L,
                durationMs = 50L,
                rowCount = 1,
                isSystem = true
        )
        val entries = listOf(
                SqlStatisticsEntry(testCase1Event, "TestCase1"),
                SqlStatisticsEntry(testCase2Event, "TestCase2")
        )

        renderer.display(entries)

        val output = logger.output()
        assertThat(output).contains("SQL Stats: TestCase1")
        assertThat(output).contains("SQL Stats: TestCase2")
        assertThat(output).contains("SELECT * FROM users")
        assertThat(output).contains("INSERT INTO logs")
    }

    @Test
    fun `should display SQL parameters`() {
        val event = createSqlEvent(
                sql = "SELECT * FROM users WHERE id = ? AND name = ?",
                parameters = listOf(123, "test_user"),
                startTimeMs = 1000L,
                durationMs = 100L
        )

        renderer.display(listOf(SqlStatisticsEntry(event, null)))

        val output = logger.output()
        assertThat(output).contains("123")
        assertThat(output).contains("test_user")
    }

    @Test
    fun `should display error status for failed queries`() {
        val event = createSqlEvent(
                sql = "SELECT * FROM invalid_table",
                startTimeMs = 1000L,
                durationMs = 100L,
                error = Exception("Table not found")
        )

        renderer.display(listOf(SqlStatisticsEntry(event, null)))

        val output = logger.output()
        assertThat(output).contains(TextColors.red("Table not found"))
    }

    @Test
    fun `should display system and user queries with different colors`() {
        val systemEvent = createSqlEvent(
                sql = "SELECT * FROM system_table",
                startTimeMs = 1000L,
                durationMs = 100L,
                isSystem = true
        )
        val userEvent = createSqlEvent(
                sql = "SELECT * FROM user_table",
                startTimeMs = 2000L,
                durationMs = 100L,
                isSystem = false
        )

        renderer.display(listOf(
                SqlStatisticsEntry(systemEvent, null),
                SqlStatisticsEntry(userEvent, null)
        ))

        val output = logger.output()

        assertThat(output).contains(TextColors.gray("SYSTEM".padEnd(11)))
        assertThat(output).contains(TextColors.green("USER".padEnd(11)))
    }

    @Test
    fun `should display total statistics correctly`() {
        val event1 = createSqlEvent(
                sql = "SELECT 1",
                startTimeMs = 1000L,
                durationMs = 100L,
                rowCount = 5
        )
        val event2 = createSqlEvent(
                sql = "SELECT 2",
                startTimeMs = 2000L,
                durationMs = 150L,
                rowCount = 3
        )

        renderer.display(listOf(
                SqlStatisticsEntry(event1, "TestCase"),
                SqlStatisticsEntry(event2, "TestCase")
        ))

        val output = logger.output().stripAnsiCodes()
        assertThat(output).contains("Total Queries: 2")
        assertThat(output).contains("Total Time: 250ms")
        assertThat(output).contains("Total Affected Rows: 8")
    }


    @Test
    fun `should display statistics for user and system queries correctly`() {
        val event1 = createSqlEvent(
                sql = "SELECT 1",
                startTimeMs = 1000L,
                durationMs = 100L,
                rowCount = 5,
                isSystem = true
        )
        val event2 = createSqlEvent(
                sql = "SELECT 2",
                startTimeMs = 2000L,
                durationMs = 150L,
                rowCount = 3
        )

        renderer.display(listOf(
                SqlStatisticsEntry(event1, "TestCase"),
                SqlStatisticsEntry(event2, "TestCase")
        ))

        val output = logger.output().stripAnsiCodes()

        assertThat(output).contains("User Queries: 1, User Time: 150ms, User Affected Rows: 3")
        assertThat(output).contains("System Queries: 1, System Time: 100ms, System Affected Rows: 5")
    }

    private fun createSqlEvent(
            sql: String,
            parameters: List<Any?> = emptyList(),
            startTimeMs: Long,
            durationMs: Long,
            rowCount: Int? = null,
            isSystem: Boolean = false,
            error: Exception? = null
    ) = SqlExecutionEvent(
            sql = sql,
            parameters = parameters,
            startTimeMs = startTimeMs,
            durationMs = durationMs,
            rowCount = rowCount,
            isSystem = isSystem,
            error = error
    )


    private fun String.stripAnsiCodes() = replace("\u001B\\[[;\\d]*m".toRegex(), "")

}
