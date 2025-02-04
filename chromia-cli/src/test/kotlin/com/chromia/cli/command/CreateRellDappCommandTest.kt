package com.chromia.cli.command

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import com.github.ajalt.clikt.testing.test
import java.io.File
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals

internal class CreateRellDappCommandTest {
    @TempDir
    lateinit var dir: File

    @Test
    fun minimalTemplate() {
        val dappName = "my_rell_dapp"
        val directoryName = "my-rell-dapp"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath))
        val projectDir = File(dir, directoryName)
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $DefaultChromiaModelRellVersion"))
        assertTrue(File(projectDir, "src/main.rell").exists())
        BuildCommand().test(listOf("-s", configYmlFile.absolutePath))
        TestCommand().test(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(projectDir, "build/$dappName.xml").exists())
    }

    @Test
    fun plainTemplate() {
        val dappName = "plain"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, "--template", dappName, dappName))
        val projectDir = File(dir, dappName)
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $DefaultChromiaModelRellVersion"))
        assertTrue(File(projectDir, "src/main.rell").exists())
        BuildCommand().test(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(projectDir, "build/plain.xml").exists())
    }

    @Test
    fun plainMultiTemplate() {
        val dappName = "my_dapp"
        val directoryName = "my-dapp"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, "--template", "plain-multi", directoryName))
        val projectDir = File(dir, directoryName)
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(configYmlFile.exists())
        val ignoreFile = File(projectDir, ".gitignore")
        assertTrue(ignoreFile.exists())
        assertTrue(configYmlFile.readText().contains("rellVersion: $DefaultChromiaModelRellVersion"))
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(File(projectDir, "src/main.rell").exists())
        assertTrue(File(projectDir, "src/development.rell").exists())
        assertTrue(File(projectDir, "src/my_dapp_test/blockchain_my_dapp_test.rell").exists())
        BuildCommand().test(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(projectDir, "build/$dappName.xml").exists())
        TestCommand().test(listOf("-s", configYmlFile.absolutePath))
        TestCommand().test(listOf("-s", "${configYmlFile.absolutePath} -bc $dappName"))
    }

    @Test
    fun plainLibraryTemplate() {
        val libName = "my_lib"
        val libDirectory = "my-lib"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, "--template", "plain-library", libDirectory))
        val projectDir = dir.resolve(libDirectory)
        assertTrue(projectDir.exists())
        assertTrue(projectDir.resolve("chromia.yml").exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        val libDir = projectDir.resolve("src/lib/$libName")
        assertTrue(libDir.exists())
        assertTrue(libDir.resolve("module.rell").exists())
        assertTrue(projectDir.resolve("src/tests/test_lib_$libName.rell").exists())
        BuildCommand().test(listOf("-s", projectDir.absolutePath))
    }

    //need to package the project after making changes on the resource
    @Test
    fun assetManagementTemplate() {
        val directoryName = "my-dapp"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, "--template", "asset-management", directoryName))
        val projectDir = File(dir, directoryName)

        assertTrue(projectDir.resolve("README.md").exists())
        assertTrue(projectDir.resolve("chromia.yml").exists())
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        assertTrue(projectDir.resolve(".gitignore").exists())
        assertTrue(projectDir.resolve("tsconfig.json").exists())
        assertTrue(projectDir.resolve("package.json").exists())
        assertTrue(projectDir.resolve(".eslintrc.json").exists())
        assertTrue(projectDir.resolve(".env.example").exists())
        assertTrue(projectDir.resolve("next.config.mjs").exists())
        assertTrue(projectDir.resolve("pnpm-lock.yaml").exists())
        assertTrue(projectDir.resolve("postcss.config.mjs").exists())
        assertTrue(projectDir.resolve("prettier.config.js").exists())
        assertTrue(projectDir.resolve("tailwind.config.ts").exists())
        assertTrue(projectDir.resolve("src").isDirectory)
        assertTrue(projectDir.resolve("public").isDirectory)
        assertTrue(projectDir.resolve("src/README.md").exists())
        assertTrue(projectDir.resolve("src/wagmi-config.ts").exists())

        val rellDir = File(projectDir, "rell")
        assertTrue(rellDir.resolve("src/main.rell").exists())
        assertTrue(rellDir.resolve("src/test").isDirectory)
        assertTrue(rellDir.resolve("README.md").exists())
    }

    @Test
    fun minimalTemplateCreatesNewFilesWithCustomName() {
        val dappName = "new_name"
        val directoryName = "new-name"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, directoryName))
        val projectDir = File(dir, directoryName)
        val configYmlFile = File(projectDir, "chromia.yml")
        assertTrue(File(projectDir, "src/main.rell").exists())
        assertThat(configYmlFile.readText()).all {
            contains("""
        blockchains:
          $dappName:
            module: main
        """.trimIndent())
            contains("schema: schema_$dappName")
        }
        assertTrue(projectDir.resolve(".rell_lint").exists())
        assertTrue(projectDir.resolve(".rell_format").exists())
        BuildCommand().test(listOf("-s", configYmlFile.absolutePath))
        assertTrue(File(projectDir, "build/$dappName.xml").exists())
    }

    @Test
    fun alreadyExistingPackageInDirectory() {
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath))
        val output = CreateRellDappCommand().test(listOf("-d", dir.absolutePath))
        assertThat(output.stdout).contains("There already exist a directory called \"my-rell-dapp\" in the working directory, aborting.")
    }

    @Test
    fun hyphensAreReplacedWithUnderscoresInDappName() {
        val dappName = "my_rell_dapp"
        val directoryName = "my-rell-dapp"
        CreateRellDappCommand().test(listOf("-d", dir.absolutePath, directoryName))
        val projectDir = File(dir, directoryName)
        assertTrue(projectDir.exists())
        val configYmlFile = File(projectDir, "chromia.yml")
        assertEquals(projectDir.name, directoryName)
        assertThat(configYmlFile.readText()).all {
            contains("""
            blockchains:
              ${dappName}:
                module: main
            compile:
              rellVersion: $DefaultChromiaModelRellVersion
            database:
              schema: schema_${dappName}
            test:
              modules:
                - test
        """.trimIndent())
        }
    }
}
