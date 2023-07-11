package com.chromia.cli.it

import com.chromia.cli.util.testData
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class CodegenIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        ChrProcess.Builder("generate-client-stubs", "--kotlin", "--package", "com.example")
                .setConfig(dir.resolve("config.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/stubs/main/main.kt")).exists()
                }
    }
}
