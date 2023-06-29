package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.none
import org.junit.jupiter.api.Test
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class VersionIT {
    @Test
    fun versionsAreIncluded() {
        val process = ProcessBuilder("./chr", "--version")
                .apply {
                    redirectErrorStream(true)
                }.start()
        process.waitFor(5, TimeUnit.SECONDS)
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val lines = reader.readLines()
        assertThat(lines[0]).contains("chr version ${System.getenv()["PROJECT_VERSION"]}")
        assertThat(lines[1]).contains("rell version")
        assertThat(lines[2]).contains("postchain version")
        assertThat(lines).none { it.contains("(unknown)") }
    }
}