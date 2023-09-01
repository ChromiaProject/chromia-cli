package com.chromia.cli

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.model.RellVersion
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class CreateRellDappCommandTest {
    @TempDir
    @JvmField
    var dir: File? = null

    @Test
    fun initCreatesNewFiles() {
        CreateRellDappCommand().parse(listOf("-d", dir!!.absolutePath))
        val configYmlFile = File(dir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(dir, "src/main.rell").exists())
        BuildCommand().parse(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun initCreatesNewFilesWithCustomName() {
        CreateRellDappCommand().parse(listOf("-d", dir!!.absolutePath, "new-name"))
        val configYmlFile = File(dir, "chromia.yml")
        assertTrue(File(dir, "src/main.rell").exists())
        assertThat(configYmlFile.readText()).all {
            contains("""
        blockchains:
          new-name:
            module: main
        """.trimIndent())
            contains("schema: schema_new_name")
        }
    }
}
