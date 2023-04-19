package com.chromia.cli.parser

import assertk.assert
import net.postchain.gtv.yaml.GtvYaml
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString

internal class IncludeYamlTest {

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
        assert(res["b"] is Map<*, *>)
        assert((res["b"] as Map<String, Int>)["a"]).equals(13)
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
        assert(res["b"]).equals(13)
    }
}
