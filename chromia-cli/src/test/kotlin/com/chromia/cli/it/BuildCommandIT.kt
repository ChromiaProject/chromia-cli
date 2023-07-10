package com.chromia.cli.it

import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BuildCommandIT {
    @Test
    fun buildWithExplicitFile(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("build")
                .setConfig(dir.resolve("config.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }

    @Test
    fun findsLegacyFile(@TempDir dir: Path) {
        TestDataCreator.basicApp(dir)
        ChrProcess.Builder("build")
                .setWorkingDir(dir.toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }
}
