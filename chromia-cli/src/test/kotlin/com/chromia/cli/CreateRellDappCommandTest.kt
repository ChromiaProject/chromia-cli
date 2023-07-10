package com.chromia.cli

import com.chromia.cli.model.RellVersion
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

internal class CreateRellDappCommandTest {
    @TempDir
    @JvmField
    var dir: File? = null

    @Test
    fun initCreatesNewFiles() {
        CreateRellDappCommand().parse(listOf("-d", dir!!.absolutePath))
        val configYmlFile = File(dir, "config.yml")
        assertTrue(configYmlFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(dir, "src/main.rell").exists())
        BuildCommand().parse(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun initCreatesNewFilesWithCustomName() {
        CreateRellDappCommand().parse(listOf("-d", dir!!.absolutePath, "newName"))
        val configYmlFile = File(dir, "config.yml")
        assertTrue(File(dir, "src/main.rell").exists())
        configYmlFile.readText().contains("""
        blockchains:
            newName:
                module: main
        """.trimIndent())
    }
}
