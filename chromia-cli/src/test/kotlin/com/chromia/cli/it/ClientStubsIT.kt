package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.exists
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import java.nio.file.Path
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class ClientStubsIT {
    @Test
    fun generateKotlinClientStubs(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("generate-client-stubs", "--kotlin", "--package", "com.example")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    assertThat(dir.resolve("build/generated/main/main.kt")).exists()
                }
    }
}
