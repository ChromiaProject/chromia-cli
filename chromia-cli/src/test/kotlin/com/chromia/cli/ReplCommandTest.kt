package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import com.chromia.cli.util.InitExtension
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ReplCommandTest {
    @JvmField
    @RegisterExtension
    val command = InitExtension()
    val dir get() = command.dir

    @Test
    fun testCanNotConnectToDb() {
        EnvironmentVariables("CHR_DB_URL", "jdbc:postgresql://invalidhost/postgres").execute {
            assertThrows<PSQLException> {
                ReplCommand().test("--module=main --use-db -s ${dir.absolutePath}/chromia.yml")
            }
        }
    }

    @Test
    fun testCanNotFindModuleWithoutSettings() {
        val res = ReplCommand().test("--module=main")
        assertThat(res.output).contains("To find the module \"main\", specifying the settings file is required")
    }

    @Test
    fun testCanNotConnectToDbWithoutSettings() {
        EnvironmentVariables("CHR_DB_URL", "").execute {
            val res = ReplCommand().test("--use-db")
            assertThat(res.stderr).contains("To correctly connect to the database, specifying the settings file is required")
        }
    }

    @Test
    fun commandLineInput() {
        val res = ReplCommand().test("-c '5+5'")
        assertThat(res.output).doesNotContain("Rell")
        assertThat(res.output).contains("10")
    }
}
