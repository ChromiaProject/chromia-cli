package com.chromia.cli.command.generate

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

internal class GenerateDocsSiteTest {

    @Test
    fun testModuleFilter() {
        val libs = setOf("lib.a", "lib.b", "lib.c", "lib.d", "lib.e")
        val includes = listOf("lib.b", "lib.c")

        val res = GenerateDocsSiteCommand().getFilteredModules(libs, includes)
        assertThat(res.size).isEqualTo(3)
        assertThat(res).containsAll("lib.a", "lib.d", "lib.e")
    }

    @Test
    fun `should hide library warnings with hide-lib-warnings option`(@TempDir dir: Path) {
        val projectResourceUrl = javaClass.classLoader.getResource("dapp_with_libWarnings")
            ?: fail { "dapp not found under resources" }
        val projectPath = Paths.get(projectResourceUrl.toURI())
        val terminalRecorder = TerminalRecorder()
        val terminal = Terminal(terminalInterface = terminalRecorder)

        val docsTargetDir = File(projectPath.toFile(), "docs").apply { mkdirs() }

        GenerateDocsSiteCommand().context { this.terminal = terminal }
            .parse(
                listOf(
                    "--settings",
                    projectPath.resolve("chromia.yml").toString(),
                    "--target",
                    docsTargetDir.absolutePath
                )
            )

        val outputWithoutHiding = terminalRecorder.stderr()
        terminalRecorder.clearOutput()

        GenerateDocsSiteCommand().context { this.terminal = terminal }
            .parse(
                listOf(
                    "--settings",
                    projectPath.resolve("chromia.yml").toString(),
                    "--target",
                    docsTargetDir.absolutePath,
                    "--hide-lib-warnings"
                )
            )

        val outputWithHiding = terminalRecorder.stderr()

        assertThat(outputWithoutHiding).contains("lib/testlib")
        assertThat(outputWithHiding).doesNotContain("lib/testlib")

        assertThat(outputWithoutHiding).contains("Lib Warnings: 1")
        assertThat(outputWithHiding).contains("Lib Warnings: 1")
    }
}
