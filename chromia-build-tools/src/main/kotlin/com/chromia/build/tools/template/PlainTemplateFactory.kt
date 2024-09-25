package com.chromia.build.tools.template

import java.io.File

/**
 * Creates an empty single-module project with a main and a test file.
 */
class PlainTemplateFactory : AbstractTemplateFactory("plain") {
    override fun createProjectFromTemplate(targetDir: File, projectName: String) {
        val projectFileName = snakeCaseName(projectName)
        with(FileBuilder(targetDir)) {
            createChromiaConfig(projectName)
            createGitIgnore()
            createFormatterConfig()
            createLinterConfig()
            createFile("src/main.rell")
            createFile("src/test/plain_test.rell", "src/test/${projectFileName}_test.rell")
        }
    }
}