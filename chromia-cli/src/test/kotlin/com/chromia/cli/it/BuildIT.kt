package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class BuildIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("build")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }

    @Test
    fun findsLegacyFile(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("build")
                .setWorkingDir(dir.toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }
}
