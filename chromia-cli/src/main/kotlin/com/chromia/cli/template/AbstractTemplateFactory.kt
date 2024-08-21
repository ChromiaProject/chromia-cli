package com.chromia.cli.template

import com.chromia.cli.model.DefaultChromiaModelRellVersion
import java.io.File

abstract class AbstractTemplateFactory(private val folderName: String) : TemplateFactory {

    protected fun snakeCaseName(string: String) = string.replace("-", "_")

    protected inner class FileBuilder(private val targetDir: File) {
        fun createFile(sourceName: String, targetName: String = sourceName, transform: (String) -> String = { it }) {
            with(File(targetDir, targetName)) {
                if (!parentFile.exists()) parentFile.mkdirs()
                val fileContent = AbstractTemplateFactory::class.java.getResource("$folderName/$sourceName")!!.readText()
                writeText(transform(fileContent))
            }
        }

        fun createChromiaConfig(projectName: String, transform: (String) -> String = { it }) {
            createFile("chromia.yml") {
                it.replace("PROJECT_NAME", projectName)
                        .replace("RELL_VERSION", DefaultChromiaModelRellVersion)
                        .replace("RELL_SCHEMA", "schema_${snakeCaseName(projectName)}")
                        .let(transform)
            }
        }

        fun createGitIgnore(init: () -> String = { "" }) {
            val sourceName = ".gitignore"
            with(File(targetDir, sourceName)) {
                val fileContent = AbstractTemplateFactory::class.java.getResource(sourceName)!!.readText()
                writeText(fileContent)
                appendText(init())
            }
        }

        fun createLinterConfig(init: () -> String = { "" }) {
            val sourceName = ".rell_lint"
            with(File(targetDir, sourceName)) {
                val fileContent = AbstractTemplateFactory::class.java.getResource(sourceName)!!.readText()
                writeText(fileContent)
                appendText(init())
            }
        }

        fun createFormatterConfig(init: () -> String = { "" }) {
            val sourceName = ".rell_format"
            with(File(targetDir, sourceName)) {
                val fileContent = AbstractTemplateFactory::class.java.getResource(sourceName)!!.readText()
                writeText(fileContent)
                appendText(init())
            }
        }
    }
}