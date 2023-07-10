package com.chromia.cli.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class BuildCommandIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("build")
                .setConfig(dir.resolve("config.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }
}
