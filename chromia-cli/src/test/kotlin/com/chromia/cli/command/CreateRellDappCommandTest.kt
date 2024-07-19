package com.chromia.cli.command

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
    lateinit var dir: File

    @Test
    fun minimalTemplate() {
        CreateRellDappCommand().test("-d ${dir.absolutePath}")
        val projectDir = File(dir, "my-rell-dapp")
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(projectDir, "src/main.rell").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        TestCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(projectDir, "build/my-rell-dapp.xml").exists())
    }

    @Test
    fun plainTemplate() {
        CreateRellDappCommand().test("-d ${dir.absolutePath} --template plain plain")
        val projectDir = File(dir, "plain")
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(File(projectDir, "src/main.rell").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(projectDir, "build/plain.xml").exists())
    }

    @Test
    fun plainMultiTemplate() {
        CreateRellDappCommand().test("-d ${dir.absolutePath} --template plain-multi my-dapp")
        val projectDir = File(dir, "my-dapp")
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $RellVersion"))
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(File(projectDir, "src/main.rell").exists())
        assertTrue(File(projectDir, "src/development.rell").exists())
        assertTrue(File(projectDir, "src/my_dapp_test/blockchain_my_dapp_test.rell").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(projectDir, "build/my-dapp.xml").exists())
        TestCommand().test("-s ${configYmlFile.absolutePath}")
        TestCommand().test("-s ${configYmlFile.absolutePath} -bc my-dapp")
    }

    @Test
    fun plainLibraryTemplate() {
        CreateRellDappCommand().test("-d ${dir.absolutePath} --template plain-library my-lib")
        val projectDir = dir.resolve("my-lib")
        assertTrue(projectDir.exists())
        assertTrue(projectDir.resolve("chromia.yml").exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        val libDir = projectDir.resolve("src/lib/my_lib")
        assertTrue(libDir.exists())
        assertTrue(libDir.resolve("module.rell").exists())
        assertTrue(projectDir.resolve("src/tests/test_lib_my_lib.rell").exists())
        BuildCommand().test("-s ${projectDir.absolutePath}")
    }

    @Test
    fun minimalTemplateCreatesNewFilesWithCustomName() {
        CreateRellDappCommand().test("-d ${dir.absolutePath} new-name")
        val projectDir = File(dir, "new-name")
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(File(projectDir, "src/main.rell").exists())
        assertThat(configYmlFile.readText()).all {
            contains("""
        blockchains:
          new-name:
            module: main
        """.trimIndent())
            contains("schema: schema_new_name")
        }
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        BuildCommand().test("-s ${configYmlFile.absolutePath}")
        assertTrue(File(projectDir, "build/new-name.xml").exists())
    }

    @Test
    fun alreadyExistingPackageInDirectory() {
        CreateRellDappCommand().test("-d ${dir.absolutePath}")
        val output = CreateRellDappCommand().test("-d ${dir.absolutePath}")
        assertThat(output.stdout).contains("There already exist a directory called \"my-rell-dapp\" in the working directory, aborting.")
    }
}
