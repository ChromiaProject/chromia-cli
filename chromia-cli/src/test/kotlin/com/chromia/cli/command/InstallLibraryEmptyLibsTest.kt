package com.chromia.cli.command

import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.command.library.management.InstallLibraryCommand
import com.chromia.cli.util.TestRepositoryCloner
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Verifies that `chr library install` (and its `chr install` alias) does not attempt
 * to connect to the library chain when `chromia.yml` declares no libraries.
 *
 * Lives in its own class because the broader [InstallLibraryCommandTest] is currently
 * `@Disabled` while the test fixtures are being migrated to the `ChromiaPredefinedNetworks`
 * API; this scenario does not need that fixture because the empty-libs early return
 * runs before the network client is initialized.
 */
class InstallLibraryEmptyLibsTest {

    private val logger = TerminalRecorder()
    private val testTerminal = Terminal(terminalInterface = logger)

    @TempDir
    private lateinit var testDir: Path

    @Test
    fun `does not initialize library chain client when libs is empty`() {
        val settingsFile = testDir.resolve("chromia.yml").toFile()
        settingsFile.writeText("""
            blockchains:
                my_rell_dapp:
                  module: main
        """.trimIndent())

        assertDoesNotThrow {
            InstallLibraryCommand { TestRepositoryCloner() }
                    .context { terminal = testTerminal }
                    .parse(listOf("-s", settingsFile.absolutePath))
        }

        assertThat(logger.output()).contains("No libraries found in: ${settingsFile.absolutePath}")
    }
}
