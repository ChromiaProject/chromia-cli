package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.startsWith
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class MermaidGraphIT {
    @Test
    fun generateMermaidGraph(@TempDir dir: Path) {
        testData(dir) {
            content("module; entity my_entity { name; }")
        }
        TestProcess.Builder("generate", "graph", "-m", "main")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    val graph = dir.resolve("build/graph/rell.mmd")
                    assertThat(graph).exists()
                    assertThat(graph.toFile().readText()).startsWith("\nerDiagram")
                }
    }
}
