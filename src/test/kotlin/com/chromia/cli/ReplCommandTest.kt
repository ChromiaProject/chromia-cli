package com.chromia.cli

import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReplCommandTest {

    val testConsole = TestConsole()

    @RegisterExtension
    private val command = CommandExtension(ReplCommand().context { console = testConsole })
    val dir get() = command.dir

    @Test
    fun testCanNotConnectToDb() {
        File(command.dir, "config.yml").appendText("""
            database:
              host: invalidhost
        """.trimIndent())
        command.parse(listOf("--module=main", "--use-db"))
        testConsole.assertContains("The connection attempt failed.\n")
    }
    @Test
    fun testCanNotFindModuleWithoutSettings() {
        command.emptyParse(listOf("--module=main"))
        testConsole.assertContains("To find the module \"main\", specifying the settings file is required\n")
    }

    @Test
    fun testCanNotConnectToDbWithoutSettings() {
        val exception = assertFailsWith<CliktError> {
            command.emptyParse(listOf("--use-db"))
        }
        assertEquals("To correctly connect to the database, specifying the settings file is required", exception.message)
    }
}
