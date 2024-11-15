package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readText

class GenerateDocsIT {

    @Test
    fun `Test that command runs with default values for docs`(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("generate", "docs-site")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("Documentation generated at")
                .start()

        val indexFile = dir.resolve("build/site/index.html")
        assertThat(indexFile).exists()
    }

    @Test
    fun `Test that plain text attributes gets added to html`(@TempDir dir: Path) {
        testData(dir) {
            config {
                docs("""
                    docs:
                      title: my_dapp
                      footerMessage: my_footer
            """.trimIndent())
            }
        }

        TestProcess.Builder("generate", "docs-site")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("Documentation generated at")
                .start()

        val indexFile = dir.resolve("build/site/index.html")
        assertThat(indexFile).exists()
        val indexFileContent = indexFile.readText()
        assertThat(indexFileContent).contains("my_dapp")
        assertThat(indexFileContent).contains("my_footer")
    }

    @Test
    fun `Test that source links gets added to html`(@TempDir dir: Path) {
        testData(dir) {
            config {
                docs("""
                     docs:
                       title: My Dapp
                       sourceLink:
                         remoteUrl: https://github.com/chromia/foobar/src
                         remoteLineSuffix: "#L"
             """.trimIndent())
            }
        }

        TestProcess.Builder("generate", "docs-site")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("Documentation generated at")
                .start()

        val callOpContent = dir.resolve("build/site/-my -dapp/main/call_op.html").readText()
        assertThat(callOpContent).contains("https://github.com/chromia/foobar/src/main.rell#L4")
    }

    @Test
    fun `Test that additional properties gets added to generation`(@TempDir dir: Path) {
        testData(dir) {
            config {
                docs("""
                    docs:
                      title: my_dapp
                      customStyleSheets:
                        - ${dir.toAbsolutePath()}/src/customStyleSheet.css
                      customAssets:
                        - ${dir.toAbsolutePath()}/src/customAsset.png
                      additionalContent:
                        - ${dir.toAbsolutePath()}/src/customInclude.md 
                        
            """.trimIndent())
            }
            addSourceFile("customStyleSheet.css", """
                .navigation {
                    background-color: green;
                }
            """.trimIndent()
            )
            addSourceFile("customAsset.png", """
                I am an image
            """.trimIndent()
            )
            addSourceFile("customInclude.md", """
                # Dapp my_dapp
                My main text
                # Module main
                My custom comment for module main
            """.trimIndent()
            )
        }

        val targetDir = "${dir.toAbsolutePath()}/build"
        TestProcess.Builder("generate", "docs-site")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("Documentation generated at")
                .start()

        assertThat(File(targetDir, "site/styles/customStyleSheet.css")).exists()
        assertThat(File(targetDir, "site/images/customAsset.png")).exists()
        val indexFileContent = File(targetDir, "site/index.html").readText()
        assertThat(indexFileContent).contains("My main text")
        assertThat(indexFileContent).contains("My custom comment for module main")
    }

    @Test
    fun `Generating system docs requires target folder`(@TempDir dir: Path) {
        TestProcess.Builder("generate", "docs-site", "--system")
                .exitCode(3)
                .setWorkingDir(dir.toFile())
                .awaitCompletion(true)
                .start()
    }

    @Test
    fun `Generating system docs does not require model file`(@TempDir dir: Path) {
        val docFile = File(dir.toFile(), "extra.md").apply {
            writeText("""
                # Package rell
                
                Sample docs content
            """.trimIndent())
        }

        TestProcess.Builder("generate", "docs-site", "--system", "--target", "${dir.absolutePathString()}/site", "--system-include", docFile.absolutePath)
                .setWorkingDir(dir.toFile())
                .awaitCompletion(true)
                .start()
        assertThat(File(dir.toFile(), "site/index.html")).exists()
    }
}