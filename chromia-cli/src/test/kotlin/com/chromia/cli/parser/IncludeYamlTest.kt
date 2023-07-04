package com.chromia.cli.parser

import assertk.assertThat
import net.postchain.gtv.yaml.GtvYaml
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
        val res = GtvYaml().loadAnchor<Map<String, Any>>(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is Map<*, *>)
        assertThat((res["b"] as Map<String, Int>)["a"]).isEqualTo(13)
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
        val res = GtvYaml().loadAnchor<Map<String, Any>>(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is List<*>)
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
        val res = GtvYaml().loadAnchor<Map<String, Any>>(File(dir.toFile(), "b.yml"))
        assertThat(res["b"] is Map<*, *>)
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
        val res = GtvYaml().loadAnchor<Map<String, Int>>(File(dir.toFile(), "b.yml"))
        assertThat(res["b"]).isEqualTo(13)
    }
}
