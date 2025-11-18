package com.chromia.cli.util

import assertk.Assert
import assertk.all
import assertk.assertThat
import assertk.assertions.*
import assertk.assertions.support.expected
import assertk.assertions.support.show
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.yaml.snakeyaml.Yaml
import java.io.File
import java.nio.file.Path

@Suppress("UNCHECKED_CAST")
class ChromiaYamlUpdaterTest {

    @TempDir
    lateinit var tempDir: Path

    private val yaml = Yaml()

    private fun assertLibraryVersion(libs: Map<String, Any>, libName: String, expectedVersion: String) {
        val lib = libs[libName] as? Map<String, Any> ?: error("Library $libName not found or is not a map")
        assertThat(lib).all {
            containsKey("version")
            prop("version").isEqualTo(expectedVersion)
        }
    }

    private fun <K, V> Assert<Map<K, V>>.containsKey(key: K): Assert<Map<K, V>> =
            transform("contains key ${show(key)}") { actual ->
                if (!actual.containsKey(key)) {
                    expected("to contain key ${show(key)} but was ${show(actual)}")
                }
                actual
            }

    private fun Assert<Map<String, Any>>.prop(name: String): Assert<Any?> = transform(name) { actual -> actual[name] }


    @Test
    fun `should add libs section with library when it doesn't exist`() {
        val yamlContent = """
            blockchains:
              chain_1:
                module: main
            test:
              modules:
              - test
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia.yml").apply {
            writeText(yamlContent)
        }

        updateChromiaYamlForLibrary(yamlFile, "com.chromia.ft4", "1.0.0")

        val updatedContent = yamlFile.readText()
        val parsed = yaml.load<Map<String, Any>>(updatedContent)

        assertThat(parsed).all {
            hasSize(3)
            containsKey("libs")
            containsKey("blockchains")
        }

        val libs = parsed["libs"] as? Map<String, Any> ?: error("libs is not a map")
        assertLibraryVersion(libs, "com.chromia.ft4", "1.0.0")
    }

    @Test
    fun `should update existing library version in main file`() {
        val yamlContent = """
            libs:
              com.chromia.ft4:
                version: "0.9.0"
              another.lib:
                version: "2.0.0"
            blockchains:
              blockchain_1:
                module: main
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia-with-libs.yml").apply {
            writeText(yamlContent)
        }

        updateChromiaYamlForLibrary(yamlFile, "com.chromia.ft4", "1.0.0")

        val updatedContent = yamlFile.readText()
        val parsed = yaml.load<Map<String, Any>>(updatedContent)

        val libs = parsed["libs"] as? Map<String, Any> ?: error("libs is not a map")
        assertLibraryVersion(libs, "com.chromia.ft4", "1.0.0")
        assertLibraryVersion(libs, "another.lib", "2.0.0")
    }

    @Test
    fun `should handle include directive and update referenced file`() {
        val configDir = File(tempDir.toFile(), "config").apply { mkdirs() }
        
        val mainYamlContent = """
            libs: !include 'config/libs.yml'
            blockchains:
              chain_1:
                module: main
        """.trimIndent()

        val libsYamlContent = """
            ft4:
              registry: https://gitlab.com/chromaway/ft4-lib.git
              path: rell/src/lib/ft4
              tagOrBranch: v1.0.0r
              rid: x"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F"
              insecure: false
        """.trimIndent()

        val mainYamlFile = File(tempDir.toFile(), "chromia-include.yml").apply {
            writeText(mainYamlContent)
        }
        val libsYamlFile = File(configDir, "libs.yml").apply {
            writeText(libsYamlContent)
        }

        updateChromiaYamlForLibrary(mainYamlFile, "com.chromia.ft4", "1.100.0")

        val mainContent = mainYamlFile.readText()
        assertThat(mainContent).contains("config/libs.yml")

        val libsContent = libsYamlFile.readText()
        val libsParsed = yaml.load<Map<String, Any>>(libsContent)

        assertLibraryVersion(libsParsed, "com.chromia.ft4", "1.100.0")
        assertThat(libsParsed).all {
            hasSize(2)
            containsKey("ft4")
            containsKey("com.chromia.ft4")
        }

        val originalGitFt4 = libsParsed["ft4"] as? Map<String, Any> ?: error("ft4 is not a map")
        assertThat(originalGitFt4).all {
            hasSize(5)
            prop("registry").isEqualTo("https://gitlab.com/chromaway/ft4-lib.git")
            prop("path").isEqualTo("rell/src/lib/ft4")
            prop("tagOrBranch").isEqualTo("v1.0.0r")
            prop("rid").isEqualTo("x\"FA487D75E63B6B58381F8D71E0700E69BEDEAD3A57D1E6C1A9ABB149FAC9E65F\"")
            prop("insecure").isEqualTo(false)
        }
    }

    @Test
    fun `should update library in included file when it already exists`() {
        val configDir = File(tempDir.toFile(), "config").apply { mkdirs() }
        
        val mainYamlContent = """
            libs: !include 'config/existing-libs.yml'
            blockchains:
              chain_1:
                module: main
        """.trimIndent()

        val existingLibsContent = """
            com.chromia.ft4:
              version: "0.5.0"
            ft4:
              registry: https://gitlab.com/chromaway/ft4-lib.git
              path: rell/src/lib/ft4
            another.lib:
              version: "3.0.0"
        """.trimIndent()

        val mainYamlFile = File(tempDir.toFile(), "chromia-existing-include.yml").apply {
            writeText(mainYamlContent)
        }
        val libsYamlFile = File(configDir, "existing-libs.yml").apply {
            writeText(existingLibsContent)
        }

        updateChromiaYamlForLibrary(mainYamlFile, "com.chromia.ft4", "1.5.0")

        val libsContent = libsYamlFile.readText()
        val libsParsed = yaml.load<Map<String, Any>>(libsContent)
        
        assertThat(libsParsed).all {
            hasSize(3)
            containsKey("com.chromia.ft4")
            containsKey("ft4")
            containsKey("another.lib")
        }

        assertLibraryVersion(libsParsed, "com.chromia.ft4", "1.5.0")

        assertLibraryVersion(libsParsed, "another.lib", "3.0.0")

        val originalGitFt4 = libsParsed["ft4"] as? Map<String, Any> ?: error("ft4 is not a map")
        assertThat(originalGitFt4).all {
            hasSize(2)
            prop("registry").isEqualTo("https://gitlab.com/chromaway/ft4-lib.git")
            prop("path").isEqualTo("rell/src/lib/ft4")
        }
    }

    @Test
    fun `should update library version while preserving the original YAML structure`() {
        val yamlContent = """
            # first comment
            
            
            
            # second comment
            
            libs:
              # ft4 chromia library
              com.chromia.ft4:
                version: "0.9.0"
              another.lib:
                version: "2.0.0"
            blockchains:
              blockchain_1:
                module: main
        """.trimIndent()

        val yamlFile = File(tempDir.toFile(), "chromia-with-libs.yml").apply {
            writeText(yamlContent)
        }

        updateChromiaYamlForLibrary(yamlFile, "com.chromia.ft4", "1.0.0")

        val updatedContent = yamlFile.readText()
        assertThat(updatedContent).all {
            equals("""
                # first comment



                # second comment

                libs:
                  # ft4 chromia library
                  com.chromia.ft4:
                    version: 1.0.0
                  another.lib:
                    version: "2.0.0"
                blockchains:
                  blockchain_1:
                    module: main
            """.trimIndent())
        }
        val parsed = yaml.load<Map<String, Any>>(updatedContent)

        val libs = parsed["libs"] as? Map<String, Any> ?: error("libs is not a map")
        assertLibraryVersion(libs, "com.chromia.ft4", "1.0.0")
        assertLibraryVersion(libs, "another.lib", "2.0.0")
    }

}