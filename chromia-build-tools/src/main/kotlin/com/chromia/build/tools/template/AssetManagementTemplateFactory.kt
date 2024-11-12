package com.chromia.build.tools.template

import java.io.File

/**
 * Creates an empty template with a rell backend and simple frontend to connect a wallet
 */
class AssetManagementTemplateFactory : AbstractTemplateFactory("asset-management") {
    override fun createProjectFromTemplate(targetDir: File, projectName: String) {
        with(FileBuilder(targetDir)) {
            moveFolder("")
            createFormatterConfig()
            createLinterConfig()
        }
    }
}