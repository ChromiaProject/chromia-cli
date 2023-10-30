package com.chromia.cli.template

import com.chromia.cli.model.RellVersion
import java.io.File

class MinimalTemplateFactory: TemplateFactory {
    override fun createProjectFromTemplate(projectName: String, targetDir: File) {
        File(targetDir, "chromia.yml").writeText(
                this::class.java.getResource("minimal/chromia.yml")!!.readText()
                        .replace("hello", projectName)
                        .replace("RELL_VERSION", RellVersion)
                        .replace("RELL_SCHEMA", "schema_${projectName.replace("-", "_")}")
        )
        val sourceDir = File(targetDir, "src")
        sourceDir.mkdir()
        File(sourceDir, "main.rell").writeText(
                this::class.java.getResource("minimal/src/main.rell")!!.readText()
        )
        val testDir = File(sourceDir, "test")
        testDir.mkdir()
        File(testDir, "arithmetic_test.rell").writeText(
                this::class.java.getResource("minimal/src/test/arithmetic_test.rell")!!.readText()
        )
        File(testDir, "data_test.rell").writeText(
                this::class.java.getResource("minimal/src/test/data_test.rell")!!.readText()
        )
    }
}