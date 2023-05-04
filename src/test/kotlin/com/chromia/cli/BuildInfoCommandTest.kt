package com.chromia.cli

import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class BuildInfoCommandTest {
    @Test
    fun calcBrid(@TempDir dir: Path) {
        with (File(dir.toFile(), "bc-config.xml")) {
            writeText("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <string>my_config</string>
            """.trimIndent())
        }
        val testConsole = TestConsole()
        BuildInfoCommand().context { console = testConsole }.parse(listOf("${dir.absolutePathString()}/bc-config.xml"))
        testConsole.assertContains("A6D29C440E6AA5136829F0C151111B0855DEA3FA31BD25FA6097282BEC2DCA1A")
    }
}