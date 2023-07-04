package com.chromia.cli.it

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class CodegenCommandIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("generate-client-stubs", "--kotlin", "--package", "com.example")
                .setConfig(dir.resolve("config.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/stubs/main/main.kt")).exists()
                }
    }
}
