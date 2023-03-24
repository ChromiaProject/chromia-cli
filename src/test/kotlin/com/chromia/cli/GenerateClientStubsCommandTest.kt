package com.chromia.cli

import assertk.assert
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.TestConsole
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

internal class GenerateClientStubsCommandTest {

    private val testConsole = TestConsole()

    @RegisterExtension
    val command = CommandExtension(GenerateClientStubsCommand().context { console = testConsole })
    val dir get() = command.dir

    @Test
    fun generateKotlin() {
        command.parse(listOf("-s", "${dir.absolutePath}/config.yml", "--kotlin", "--package", "com.example"))
        assert(File(dir, "build/main").listFiles()).hasSize(1)
        assert(File(dir, "build/main").list()).containsAll("main.kt")
        assert(File(dir, "build/main/main.kt").readLines()[2]).isEqualTo("package com.example.main")
        testConsole.assertContains("Created files: [main/main.kt]\n")
    }

    @Test
    fun generateTypescript() {
        command.parse(listOf("-s", "${dir.absolutePath}/config.yml", "--typescript"))
        assert(File(dir, "build/main").listFiles()).hasSize(1)
        assert(File(dir, "build/main").list()).containsAll("main.ts")
        testConsole.assertContains("Created files: [main/main.ts]\n")
    }

    @Test
    fun generateJavascript() {
        command.parse(listOf("-s", "${dir.absolutePath}/config.yml", "--javascript"))
        assert(File(dir, "build/main").listFiles()).hasSize(1)
        assert(File(dir, "build/main").list()).containsAll("main.js")
        assert(File(dir, "build/").listFiles()).hasSize(2)
        assert(File(dir, "build/").list()).containsAll("root.js")
        testConsole.assertContains("Created files: [/root.js, main/main.js]\n")
    }

    @Test
    fun moduleDependencies() {
        with(File(dir, "src/a.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                struct a1 { name; }
                struct a2 { name; }
            """.trimIndent())
        }

        with(File(dir, "src/e1.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                import ^.a.*;
                query q_a1() = a1("");
            """.trimIndent())
        }

        with(File(dir, "src/e2.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                import ^.a.*;
                query q_a2() = a2("");
            """.trimIndent())
        }

        val settings = File(dir, "config.yml").apply {
            writeText("""
                blockchains:
                  e1:
                    module: e1
                  e2:
                    module: e2
            """.trimIndent())
        }

        command.parse(listOf("-s", "${settings.absolutePath}", "--typescript"))

        assert(File(dir, "build/").listFiles()).hasSize(3)
        assert(File(dir, "build/a/a.ts").readLines()).containsAll(
                "export type A1 = {",
                "export type A2 = {")
        testConsole.assertContains("Created files: [a/a.ts, e1/e1.ts, e2/e2.ts]\n")
    }
}
