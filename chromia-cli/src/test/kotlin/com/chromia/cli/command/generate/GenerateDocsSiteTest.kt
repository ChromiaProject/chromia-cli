package com.chromia.cli.command.generate

import assertk.assertThat
import assertk.assertions.containsAll
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

internal class GenerateDocsSiteTest {

    @Test
    fun testModuleFilter() {
        val libs = setOf("lib.a", "lib.b", "lib.c", "lib.d", "lib.e")
        val includes = listOf("lib.b", "lib.c")

        val res = GenerateDocsSiteCommand().getFilteredModules(libs, includes)
        assertThat(res.size).isEqualTo(3)
        assertThat(res).containsAll("lib.a", "lib.d", "lib.e")
    }
}