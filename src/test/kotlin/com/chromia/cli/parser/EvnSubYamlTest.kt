package com.chromia.cli.parser

import assertk.assert
import assertk.assertions.isEqualTo
import net.postchain.gtv.yaml.GtvYaml
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
                writeText("a: !ENV \${MY_A}")
            }
            val res = GtvYaml().loadAnchor<Map<String, String>>(File(dir.toFile(), "a.yml"))
            assert(res["a"]).isEqualTo("foo")
        }
    }

    @Test
    fun `Environment variables can only be strings`(@TempDir dir: Path) {
        EnvironmentVariables("MY_A", "1").execute {
            with(File(dir.toFile(), "a.yml")) {
                writeText("a: !ENV \${MY_A}")
            }
            val res = GtvYaml().loadAnchor<Map<String, Int>>(File(dir.toFile(), "a.yml"))
            assert(res["a"] as String).isEqualTo("1")
            assert(res["a"])
        }
    }

}