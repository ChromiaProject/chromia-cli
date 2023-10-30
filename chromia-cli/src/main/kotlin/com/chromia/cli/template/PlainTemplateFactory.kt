package com.chromia.cli.template

import com.chromia.cli.model.RellVersion
import java.io.File

class PlainTemplateFactory: TemplateFactory {
    override fun createProjectFromTemplate(projectName: String, targetDir: File) {
        File(targetDir, "chromia.yml").writeText(
                this::class.java.getResource("plain/chromia.yml")!!.readText()
                        .replace("hello", projectName)
                        .replace("RELL_VERSION", RellVersion)
                        .replace("RELL_SCHEMA", "schema_${projectName.replace("-", "_")}")
        )
        val sourceDir = File(targetDir, "src")
        sourceDir.mkdir()
        File(sourceDir, "main.rell").writeText(
                this::class.java.getResource("plain/src/main.rell")!!.readText()
        )
        val testDir = File(sourceDir, "test")
        testDir.mkdir()
        File(testDir, "${projectName}_test.rell").writeText(
                this::class.java.getResource("plain/src/test/plain_test.rell")!!.readText()
        )
    }
}