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
        assert(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assert(File(dir, "build/stubs/main").list()).containsAll("main.kt")
        assert(File(dir, "build/stubs/main/main.kt").readLines()[2]).isEqualTo("package com.example.main")
        testConsole.assertContains("Created files in ${File(dir, "/build/stubs").absolutePath}: [main/main.kt]")
    }

    @Test
    fun generateTypescript() {
        command.parse(listOf("-s", "${dir.absolutePath}/config.yml", "--typescript"))
        assert(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assert(File(dir, "build/stubs/main").list()).containsAll("main.ts")
        testConsole.assertContains("Created files in ${File(dir, "/build/stubs").absolutePath}: [main/main.ts]")
    }

    @Test
    fun generateJavascript() {
        command.parse(listOf("-s", "${dir.absolutePath}/config.yml", "--javascript"))
        assert(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assert(File(dir, "build/stubs/main").list()).containsAll("main.js")
        assert(File(dir, "build/stubs/").listFiles()).hasSize(2)
        assert(File(dir, "build/stubs/").list()).containsAll("root.js")
        testConsole.assertContains("Created files in ${File(dir, "/build/stubs").absolutePath}: [/root.js, main/main.js]")
    }

    @Test
    fun moduleDependenciesWithUniqueness() {
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
                query q_a2() = a2("");
            """.trimIndent())
        }

        with(File(dir, "src/e2.rell")) {
            parentFile.mkdirs()
            writeText("""
                module;
                import ^.a.*;
                query q_a1() = a1("");
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

        assert(File(dir, "build/stubs/").listFiles()).hasSize(3)
        assert(File(dir, "build/stubs/a/a.ts").readLines().filter { it == "export type A1 = {" }).hasSize(1)
        assert(File(dir, "build/stubs/a/a.ts").readLines().filter { it == "export type A2 = {" }).hasSize(1)
        testConsole.assertContains("Created files in ${File(dir, "/build/stubs").absolutePath}: [a/a.ts, e1/e1.ts, e2/e2.ts]")
    }

    @Test
    fun generateWithTarget() {
        val targetDir = dir.absolutePath
        command.parse(listOf("-s", "$targetDir/config.yml", "--javascript", "--target", "$targetDir/stubs"))
        assert(File(dir, "stubs/main").listFiles()).hasSize(1)
        assert(File(dir, "stubs/main").list()).containsAll("main.js")
        assert(File(dir, "stubs/").listFiles()).hasSize(2)
        assert(File(dir, "stubs/").list()).containsAll("root.js")
        testConsole.assertContains("Created files in ${File(dir, "/stubs").absolutePath}: [/root.js, main/main.js]")

    }
}
