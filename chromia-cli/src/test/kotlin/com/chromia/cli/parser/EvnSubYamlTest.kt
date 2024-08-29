package com.chromia.cli.parser

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path

class EvnSubYamlTest {

    @Test
    fun `Environment variables are substituted`(@TempDir dir: Path) {
        EnvironmentVariables("MY_A", "foo").execute {
            with(File(dir.toFile(), "a.yml")) {
                writeText("a: \${MY_A}")
            }
            val res = loadAnchor(File(dir.toFile(), "a.yml"))
            assertThat(res["a"]).isEqualTo("foo")
        }
    }

    @Test
    fun `Environment variables can only be strings`(@TempDir dir: Path) {
        EnvironmentVariables("MY_A", "1").execute {
            with(File(dir.toFile(), "a.yml")) {
                writeText("a: \${MY_A}")
            }
            val res = loadAnchor(File(dir.toFile(), "a.yml"))
            assertThat(res["a"] as String).isEqualTo("1")
        }
    }

    @Test
    fun `Dollar sign can be escaped and not interpreted as env var`(@TempDir dir: Path) {
        with(File(dir.toFile(), "a.yml")) {
            writeText("a: \"\$escaped\$\"")
        }
        val res = loadAnchor(File(dir.toFile(), "a.yml"))
        assertThat(res["a"]).isEqualTo("\$escaped\$")
    }
}
