package com.chromia.build.tools.template

import com.chromia.build.tools.util.snakeCaseName
import com.chromia.cli.model.DefaultChromiaModelRellVersion
import java.io.File
import java.net.URL
import java.util.jar.JarFile

abstract class AbstractTemplateFactory(private val folderName: String) : TemplateFactory {
    protected inner class FileBuilder(private val targetDir: File) {
        fun createFile(sourceName: String, targetName: String = sourceName, transform: (String) -> String = { it }) {
            with(File(targetDir, targetName)) {
                if (!parentFile.exists()) parentFile.mkdirs()
                val fileContent = AbstractTemplateFactory::class.java.getResource("$folderName/$sourceName")!!.readText()
                writeText(transform(fileContent))
            }
        }


        fun moveFolder(sourceName: String) {
            with(targetDir) {
                if (!parentFile.exists()) parentFile.mkdirs()
                val folderURL = AbstractTemplateFactory::class.java.getResource("$folderName/$sourceName")
                        ?: throw IllegalStateException("Resource not found: $folderName/$sourceName")
                when (folderURL.protocol) {
                    "jar" -> {
                        jarMover(folderURL, this, sourceName)
                    }

                    "file" -> {
                        moveFiles(folderURL, this.resolve(sourceName))
                    }

                    else -> {
                        throw Exception("Unknown source type ${folderURL.protocol} when fetching template")
                    }
                }
            }
        }

        private fun jarMover(fileURL: URL, targetDir: File, sourceName: String) {
            val endIndex = fileURL.path.lastIndexOf("!")
            val startIndex = 5 // skipping the prefix of a jar  "jar:"
            val jarPath = fileURL.path.substring(startIndex, endIndex)
            JarFile(jarPath).use { jarFile ->
                val entries = jarFile.entries()

                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    val entryName = entry.name
                    // Check if the entry is within the specified directory
                    // skip the "!/" start of the substring with the index addition
                    if (entryName.startsWith(fileURL.path.substring(endIndex + 2)) && !entry.isDirectory) {
                        jarFile.getInputStream(entry).use { input ->
                            val targetFile = targetDir.resolve(entryName.substring(
                                    entryName.indexOf("$folderName/$sourceName") //find the start in the string
                                            + folderName.length // add the length of the base directory
                                            + 1 // add for the trailing slash
                            ))
                            targetFile.parentFile?.mkdirs()
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }

                        }
                    }
                }
            }
        }

        private fun moveFiles(fileURL: URL, targetDir: File) {
            val source = File(fileURL.path)

            if (!source.isDirectory) {
                throw IllegalArgumentException("Source must be a directory")
            }

            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }
            source.walk().forEach { sourceFile ->
                if (sourceFile.isDirectory) {
                    //skip
                } else {
                    sourceFile.inputStream().use { input ->
                        val targetFile = File(targetDir, sourceFile.relativeTo(source).path)
                        targetFile.parentFile?.mkdirs()
                        targetFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        }

        fun createChromiaConfig(projectName: String, transform: (String) -> String = { it }) {
            createFile("chromia.yml") {
                it.replace("PROJECT_NAME", snakeCaseName(projectName))
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