package com.chromia.cli.template

import java.io.File

/**
 * Minimal working example dapp (hello world)
 */
class MinimalTemplateFactory : AbstractTemplateFactory("minimal") {
    override fun createProjectFromTemplate(targetDir: File, projectName: String) {
        with(FileBuilder(targetDir)) {
            createChromiaConfig(projectName)
            createFile("src/main.rell")
            createFile("src/test/arithmetic_test.rell")
            createFile("src/test/data_test.rell")
        }
    }
}