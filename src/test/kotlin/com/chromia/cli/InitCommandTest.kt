package com.chromia.cli

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class InitCommandTest {
    @TempDir
    @JvmField
    var dir: File? = null

    @Test
    fun initCreatesNewFiles() {
        InitCommand().parse(listOf("-d", dir!!.absolutePath))
        assertTrue(File(dir, "config.yml").exists())
        assertTrue(File(dir, "src/main.rell").exists())
        BuildCommand().parse(listOf("-s", File(dir, "config.yml").absolutePath))
        assertTrue(File(dir, "build/hello.xml").exists())
        TestCommand().parse(listOf("-s", File(dir, "config.yml").absolutePath))
    }
}
