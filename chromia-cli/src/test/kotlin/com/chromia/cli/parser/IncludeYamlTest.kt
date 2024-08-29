package com.chromia.cli.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class IncludeYamlTest {

    @Test
    fun includeRelativeYaml(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               a: 13
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                b: !include a.yml
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        assertThat((res["b"] as Map<String, Int>)["a"]).isEqualTo(13)
    }

    @Test
    fun chainedInclusion(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               a: 13
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                !include a.yml
            """.trimIndent())
        }

        with(File(dir.toFile(), "c.yml")) {
            writeText("""
                b: !include b.yml
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "c.yml"))
        assertThat(res["b"] is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        assertThat((res["b"] as Map<String, Int>)["a"]).isEqualTo(13)
    }

    @Test
    fun includeTypesArePreserved(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               int: 1
               string: "foo"
               bigInt: 2L
               bytea: x"0000000000000000000000000000000000000000000000000000000000000000"
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                anchored: !include a.yml
                not_anchored:
                   int: 1
                   string: "foo"
                   bigInt: 2L
                   bytea: x"0000000000000000000000000000000000000000000000000000000000000000"
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "b.yml"))
        val anchored = res["anchored"] as Map<*, *>
        val notAnchored = res["not_anchored"] as Map<*, *>
        val keys = anchored.keys

        keys.forEach {
            assertThat(anchored[it] == notAnchored[it])
        }
    }

    @Test
    fun includeRelativeYamlList(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               - 13
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                b: !include a.yml
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is List<*>)
        @Suppress("UNCHECKED_CAST")
        assertThat((res["b"] as List<Int>).first()).isEqualTo(13)
    }

    @Test
    fun includeYaml(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               a: 13
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                b: !include ${dir.absolutePathString()}/a.yml
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is Map<*, *>)
        @Suppress("UNCHECKED_CAST")
        assertThat((res["b"] as Map<String, Int>)["a"]).isEqualTo(13)
    }

    @Test
    fun includeSubYaml(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("""
               a: 13
           """.trimIndent())
        }
        with(File(dir.toFile(), "b.yml")) {
            writeText("""
                b: !include ${dir.absolutePathString()}/a.yml#a
            """.trimIndent())
        }
        val res = loadAnchor(File(dir.toFile(), "b.yml"))
        assertThat(res["b"]).isEqualTo(13)
    }
}
