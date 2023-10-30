package com.chromia.cli.template

import java.io.File

interface TemplateFactory {
    fun createProjectFromTemplate(projectName: String, targetDir: File)
}