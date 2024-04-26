package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.chromia.cli.util.InitExtension
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import org.postgresql.util.PSQLException
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File

class ReplCommandTest {
    @JvmField
    @RegisterExtension
    val command = InitExtension()
    val dir get() = command.projectDir

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


    //TODO update assertion message when there is a proper warning message when a operation is called from repl
    @Test
    fun commandLineInputOperation() {
        with(File(dir, "src/main.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                
                object my_name {
                  mutable name= "World";
                 }
                
                operation set_name(name) {
                  my_name.name = name;
                }
            """.trimIndent())
        }
        val res = ReplCommand().test("--module=main -s ${dir.absolutePath}/chromia.yml -c 'set_name(\"foo\")'")
        assertThat(res.output).contains("Type rell.test.op cannot be converted to Gtv. Switch to a different output format.")
    }

    @Test
    fun commandLineInputStatusCode() {
        val res = ReplCommand().test("-c 'non_existing_entity @ {}'")
        assertThat(res.statusCode).isEqualTo(1)
        assertThat(res.output).contains("Unknown name: 'non_existing_entity'")
    }
}
