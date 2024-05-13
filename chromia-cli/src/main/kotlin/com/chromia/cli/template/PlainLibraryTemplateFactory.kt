package com.chromia.cli.template

import java.io.File

/**
 * Creates an empty template with several modules
 */
class PlainLibraryTemplateFactory : AbstractTemplateFactory("plain-library") {
    override fun createProjectFromTemplate(targetDir: File, projectName: String) {
        val projectFileName = snakeCaseName(projectName)
        with(FileBuilder(targetDir)) {
            createGitIgnore() {
                "!src/lib/$projectFileName"
            }
            createChromiaConfig(projectName) {
                it.replace("PROJECT_MODULE_NAME", projectFileName)
            }
            createFile("src/lib/plain-library/module.rell", "src/lib/$projectFileName/module.rell")
            createFile("src/tests/test_lib_plain_library.rell", "src/tests/test_lib_$projectFileName.rell")
        }
    }
}
