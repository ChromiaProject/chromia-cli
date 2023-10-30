package com.chromia.cli

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.model.RellVersion
import com.github.ajalt.clikt.testing.test
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

internal class CreateRellDappCommandTest {
    @TempDir
    @JvmField
    var dir: File? = null

    @Test
    fun minimalTemplate() {
        CreateRellDappCommand().test("-d ${dir!!.absolutePath}")
        val configYmlFile = File(dir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(dir, "src/main.rell").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        TestCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun plainTemplate() {
        CreateRellDappCommand().test("-d ${dir!!.absolutePath} --template plain")
        val configYmlFile = File(dir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(dir, "src/main.rell").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(dir, "build/hello.xml").exists())
    }

    @Test
    fun minimalTemplateCreatesNewFilesWithCustomName() {
        CreateRellDappCommand().test("-d ${dir!!.absolutePath} new-name")
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
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(dir, "build/new-name.xml").exists())
    }
}
