package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.testData
import com.chromia.cli.util.CommandExtension
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import kotlin.test.assertFailsWith

internal class CheckCompatibilityCommandTest {
    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(logger)

    @RegisterExtension
    val command = CommandExtension(CheckCompatibilityCommand().context { terminal = testTerminal })
    val dir get() = command.dir

    @Test
    fun `successfully run`() {
        CheckCompatibilityCommand().parse(arrayOf(
                "-f", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path,
                "-t", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path,
                "-bc", "testchain"))
    }

    @Test
    fun `blockchain argument required when using model file as input`() {

        testData(dir.toPath()) {}

        var exception = assertFailsWith<PrintMessage> {
            CheckCompatibilityCommand()
                    .context { terminal = testTerminal }
                    .parse(arrayOf(
                            "-f", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path,
                            "-t", dir.absolutePath.plus("/chromia.yml")
                    ))
        }
        assertThat(exception.message).isEqualTo("Blockchain (-bc, --blockchain) is required when model file is used as input since it can contain multiple blockchains")

        exception = assertFailsWith<PrintMessage> {
            CheckCompatibilityCommand()
                    .context { terminal = testTerminal }
                    .parse(arrayOf(
                            "-f", dir.absolutePath.plus("/chromia.yml"),
                            "-t", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path,
                    ))
        }
        assertThat(exception.message).isEqualTo("Blockchain (-bc, --blockchain) is required when model file is used as input since it can contain multiple blockchains")
    }

    @Test
    fun `invalid compatibility`() {

        val exception = assertFailsWith<PrintMessage> {
            CheckCompatibilityCommand()
                    .context { terminal = testTerminal }
                    .parse(arrayOf(
                            "-f", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-from-entity-without-default.xml")!!.path,
                            "-t", javaClass.getResource("/com/chromia/cli/check_compatilibity/testchain-to.xml")!!.path
                    ))
        }
        assertThat(exception.message).isEqualTo("Deployment failed:\n" +
                "New attribute 'new_attribute' of entity 'mocked_module:changed1' (meta: changed1) has no default value\n" +
                "New attribute 'new_attribute' of entity 'mocked_module1:changed2_entity_3' (meta: changed2_entity_3) has no default value\n" +
                "New attribute 'new_attribute' of entity 'mocked_module2:changed2_entity_4' (meta: changed2_entity_4) has no default value")
    }
}
