package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.none
import com.chromia.build.tools.TestProcess
import org.junit.jupiter.api.Test

class VersionIT {
    @Test
    fun versionsAreIncluded() {
        val lines = TestProcess.Builder("--version").start {
            it.readLines()
        }
        assertThat(lines[0]).contains("chr version ${System.getenv()["PROJECT_VERSION"]}")
        assertThat(lines[1]).contains("rell version")
        assertThat(lines[2]).contains("postchain version")
        assertThat(lines).none { it.contains("(unknown)") }
    }
}
