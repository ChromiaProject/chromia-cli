package com.chromia.cli.command.tools

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.build.tools.testData
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class RidCalculatorCommandTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `computes RID and prints result`() {
        testData(tempDir) {
            config {
                blockchains("""
                blockchains:
                  my:
                    module: lib.my
                    type: library
                """.trimIndent())
            }
            addSourceFile("lib/my/module.rell", """module;""")
            addSourceFile("lib/my/test/module.rell", """@test module;""")
        }

        val calculator = DirectoryHashCalculator(tempDir)
        val expectedRid = calculator.compute(tempDir, DirectoryHashCalculator.RidStrategy.LIST)
        val expectedHex = expectedRid.toHex()

        val result = RidCalculatorCommand().test(listOf("--path", tempDir.toString()))

        assertThat(result.statusCode).isEqualTo(0)
        assertThat(result.stdout).contains("The RID of the content of folder :'$tempDir' is")
        assertThat(result.stdout).contains(expectedHex)
    }
}
