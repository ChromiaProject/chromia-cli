package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.exists
import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

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

    @Test
    fun buildWithNonWhitelistedGtxModule(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        config:
                          gtx:
                            modules:
                              - "net.postchain.UnknownGTXModule"
                """.trimIndent())
            }
        }

        TestProcess.Builder("build")
                .setWorkingDir(dir.toFile())
                .start {
                    assertThat(dir.resolve("build/hello.xml")).exists()
                }
    }
}
