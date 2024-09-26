package com.chromia.build.tools.template

import com.chromia.build.tools.util.snakeCaseName
import java.io.File

/**
 * Creates an empty template with several modules
 */
class PlainMultiTemplateFactory : AbstractTemplateFactory("plain-multi") {
    override fun createProjectFromTemplate(targetDir: File, projectName: String) {
        val projectFileName = snakeCaseName(projectName)
        with(FileBuilder(targetDir)) {
            createChromiaConfig(projectName) {
                it.replace("PROJECT_MODULE_NAME", projectFileName)
            }
            createFile("src/main.rell") {
                it.replace("plain_multi", projectFileName)
            }
            createGitIgnore()
            createFormatterConfig()
            createLinterConfig()
            createFile("src/development.rell")
            createFile("src/plain-multi/module.rell", "src/$projectFileName/module.rell")
            createFile("src/plain-multi/test/plain_multi_test.rell", "src/$projectFileName/test/${projectFileName}_test.rell")
            createFile("src/test/plain_multi_test.rell", "src/${projectFileName}_test/blockchain_${projectFileName}_test.rell") {
                it.replace("PROJECT_NAME", projectName)
            }
        }
    }
}