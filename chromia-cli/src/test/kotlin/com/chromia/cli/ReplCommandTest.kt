package com.chromia.cli

import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ReplCommandTest {

    val testConsole = TestConsole()

    @RegisterExtension
    private val command = CommandExtension(ReplCommand().context { console = testConsole })
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
        command.emptyParse(listOf("--module=main"))
        testConsole.assertContains("To find the module \"main\", specifying the settings file is required")
    }

    @Test
    fun testCanNotConnectToDbWithoutSettings() {
        EnvironmentVariables("CHR_DB_URL", "").execute {
            val exception = assertFailsWith<CliktError> {
                command.emptyParse(listOf("--use-db"))
            }
            assertEquals("To correctly connect to the database, specifying the settings file is required", exception.message)
        }
    }

}
