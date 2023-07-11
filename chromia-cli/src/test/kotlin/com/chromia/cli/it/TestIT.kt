package com.chromia.cli.it

import com.chromia.cli.util.testData
import java.nio.file.Path
import java.time.Duration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class TestIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir) {
            addFile("test/test_code.rell", """
                @test module;
                import ^^.main;
                function test_main() { assert_equals(main.hello(), "Hi!"); }
            """.trimIndent())
            config {
                test("""
                    test:
                      modules:
                        - test
                """.trimIndent())
            }
        }
        ChrProcess.Builder("test")
                .setWorkingDir(dir.toFile())
                .start {
                    it.waitUntil("SUMMARY: 0 FAILED / 1 PASSED / 1 TOTAL", Duration.ofSeconds(10))
                }
    }
}
