package com.chromia.cli

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsAll
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.chromia.cli.util.CommandExtension
import com.chromia.cli.util.testData
import com.github.ajalt.clikt.core.context
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.RegisterExtension
import java.io.File

internal class GenerateClientStubsCommandTest {

    @RegisterExtension
    val command = CommandExtension(GenerateClientStubsCommand().context { })
    val dir get() = command.dir

    @Test
    fun errorMessageIncludeAllStubTargets() {
        val exception = assertThrows<IllegalArgumentException> {
            command.parse(listOf("-s", "${dir.absolutePath}/chromia.yml"))
        }
        assertThat(exception.message).isEqualTo("Missing language option: [--typescript, --javascript, --mermaid, --kotlin]")
    }


    //TODO known bug were multiple languages can be chosen but only the last one is generated

    @Disabled
    @Test
    fun errorMessageToManyStubTargets() {
        assertThrows<IllegalArgumentException> {
            command.parse(listOf("-s", "${dir.absolutePath}/chromia.yml", "--kotlin", "--typescript"))
        }
    }

    @Test
    fun generateKotlin() {
        val res = command.parse(listOf("-s", "${dir.absolutePath}/chromia.yml", "--kotlin", "--package", "com.example"))
        assertThat(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assertThat(File(dir, "build/stubs/main").list()).containsAll("main.kt")
        assertThat(File(dir, "build/stubs/main/main.kt").readLines()[2]).isEqualTo("package com.example.main")
        assertThat(res.output).contains("Created files in ${File(dir, "/build/stubs").absolutePath}: [main/main.kt]")
    }

    @Test
    fun generateTypescript() {
        val res = command.parse(listOf("-s", "${dir.absolutePath}/chromia.yml", "--typescript"))
        assertThat(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assertThat(File(dir, "build/stubs/main").list()).containsAll("main.ts")
        assertThat(res.output).contains("Created files in ${File(dir, "/build/stubs").absolutePath}: [main/main.ts]")
    }

    @Test
    fun generateJavascript() {
        val res = command.parse(listOf("-s", "${dir.absolutePath}/chromia.yml", "--javascript"))
        assertThat(File(dir, "build/stubs/main").listFiles()).hasSize(1)
        assertThat(File(dir, "build/stubs/main").list()).containsAll("main.js")
        assertThat(File(dir, "build/stubs/").listFiles()).hasSize(2)
        assertThat(File(dir, "build/stubs/").list()).containsAll("root.js")
        assertThat(res.output).contains("Created files in ${File(dir, "/build/stubs").absolutePath}: [/root.js, main/main.js]")
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

        val settings = File(dir, "chromia.yml").apply {
            writeText("""
                blockchains:
                  e1:
                    module: e1
                  e2:
                    module: e2
            """.trimIndent())
        }

        val res = command.parse(listOf("-s", "${settings.absolutePath}", "--typescript"))

        assertThat(File(dir, "build/stubs/").listFiles()).hasSize(3)
        assertThat(File(dir, "build/stubs/a/a.ts").readLines().filter { it == "export type A1 = {" }).hasSize(1)
        assertThat(File(dir, "build/stubs/a/a.ts").readLines().filter { it == "export type A2 = {" }).hasSize(1)
        assertThat(res.output).contains("Created files in ${File(dir, "/build/stubs").absolutePath}: [a/a.ts, e1/e1.ts, e2/e2.ts]")
    }

    @Test
    fun generateWithTarget() {
        val targetDir = dir.absolutePath
        val res = command.parse(listOf("-s", "$targetDir/chromia.yml", "--javascript", "--target", "$targetDir/stubs"))
        assertThat(File(dir, "stubs/main").listFiles()).hasSize(1)
        assertThat(File(dir, "stubs/main").list()).containsAll("main.js")
        assertThat(File(dir, "stubs/").listFiles()).hasSize(2)
        assertThat(File(dir, "stubs/").list()).containsAll("root.js")
        assertThat(res.output).contains("Created files in ${File(dir, "/stubs").absolutePath}: [/root.js, main/main.js]")

    }

    @Test
    fun multiModuleProjectDoNotOverwriteEachOther() {
        testData(dir.toPath()) {
            config {
                blockchains("""
                blockchains:
                  a:
                    module: foo.main
                  b:
                    module: bar.main
                """.trimIndent())
            }
            addFile("foo/main.rell", """
                module;
                query hello() = "hello";
            """.trimIndent())
            addFile("bar/main.rell", """
                module;
                query hello() = "hello";
            """.trimIndent())
        }
        val targetDir = dir.absolutePath
        val res = command.parse(listOf("-s", "$targetDir/chromia.yml", "--kotlin", "--package", "com.example"))
        assertThat(File(dir, "build/stubs/foo/main").listFiles()!!).hasSize(1)
        assertThat(File(dir, "build/stubs/bar/main").listFiles()!!).hasSize(1)
        assertThat(res.output).contains("/build/stubs: [foo/main/foo_main.kt, bar/main/bar_main.kt]")

    }
}
