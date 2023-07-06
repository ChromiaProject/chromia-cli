package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.util.CommandExtension
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertFailsWith
import org.postgresql.util.PSQLException
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ReplCommandTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)

    @RegisterExtension
    private val command = CommandExtension(ReplCommand().context { terminal = testTerminal })
    val dir get() = command.dir

    @Test
    fun testCanNotConnectToDb() {
        EnvironmentVariables("CHR_DB_URL", "jdbc:postgresql://invalidhost/postgres").execute {
            assertFailsWith<PSQLException> {
                command.parse(listOf("--module=main", "--use-db"))
            }
        }
    }

    @Test
    fun testCanNotFindModuleWithoutSettings() {
        val res = command.emptyParse(listOf("--module=main"))
        assertThat(res.output).contains("To find the module \"main\", specifying the settings file is required")
    }

    @Test
    fun testCanNotConnectToDbWithoutSettings() {
        EnvironmentVariables("CHR_DB_URL", "").execute {
            val res = command.emptyParse(listOf("--use-db"))
            assertThat(res.stderr).contains("To correctly connect to the database, specifying the settings file is required")
        }
    }

}