package com.chromia.cli.command

import com.chromia.build.tools.template.TemplateFactoryProvider
import com.chromia.build.tools.template.TemplateOptions
import com.chromia.build.tools.template.TemplateProject
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

class CreateRellDappCommand : ChromiaCommand(name = "create-rell-dapp", help = """
    Generates a template project
    
    Template projects:
    ${"\u0085"}Minimal - Minimal working example including sample queries/operations and tests.
    ${"\u0085"}Plain - A plain skeleton with empty main and test files.
    ${"\u0085"}Plain-Multi - A plain skeleton with empty main and test files using multiple modules.
    ${"\u0085"}Plain-library - A plain skeleton with structure for library development
    ${"\u0085"}Asset Management - A template focused on asset management on the Chromia blockchain. It includes components for blockchain operations and a frontend for user interaction
""".trimIndent()) {
    private val projectName by argument(help = "Dapp name", name = "name").default("my-rell-dapp")


    private val baseDir by option("-d", "--base-dir", help = "Directory to generate template project in")
            .file(canBeFile = false)
            .default(File(System.getProperty("user.dir")))

    private val template by option(help = "Project template")
            .enum<TemplateProject> { it.name.lowercase().replace("_", "-") }
            .default(TemplateProject.MINIMAL)

    private val includeDevContainer by option("--devcontainer", help = "Setup devcontainer for project").flag()

    override fun run() {

        if (projectName.contains(" ")) {
            throw PrintMessage("The project name can not contain blank spaces", statusCode = 1)
        }

        val projectDir = baseDir.resolve(projectName)

        if (projectDir.exists()) {
            throw PrintMessage("There already exist a directory called \"$projectName\" in the working directory, aborting.", statusCode = 1)
        }

        projectDir.mkdirs()
        val factory = TemplateFactoryProvider.getFactory(template)
        val options = TemplateOptions(includeDevContainer)
        factory.createProjectFromTemplate(projectDir, projectName, options)
    }
}
