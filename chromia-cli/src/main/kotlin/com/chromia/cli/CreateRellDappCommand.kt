package com.chromia.cli

import com.chromia.cli.template.MinimalTemplateFactory
import com.chromia.cli.template.PlainTemplateFactory
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.util.Scanner

class CreateRellDappCommand : CliktCommand(name = "create-rell-dapp", help = """
    Generates a template project
    
    Template projects:
    ${"\u0085"}Minimal - Minimal working example including sample queries/operations and tests.
    ${"\u0085"}Plain - A plain skeleton with empty main and test files.
""".trimIndent()) {
    private val name by argument(help = "Dapp name").default("hello")

    private val baseDir by option("-d", "--base-dir", help = "Directory to generate template project in")
            .file(canBeFile = false)
            .default(File(System.getProperty("user.dir")))

    private val template by option(help = "Project template").enum<TemplateProject>().default(TemplateProject.MINIMAL)

    override fun run() {

        if (!baseDir.exists()) baseDir.mkdirs()
        if (File(baseDir, "chromia.yml").exists()) {
            echo("A chromia.yml file exists in the working directory. Would you like to write over it? (All file content will be lost) Type DELETE to proceed with the deletion")
            val resp = Scanner(System.`in`).nextLine().equals("DELETE", false)
            if (resp) {
                File(baseDir, "chromia.yml").delete()
            } else {
                return
            }
        }
        val factory = when (template) {
            TemplateProject.PLAIN -> PlainTemplateFactory()
            TemplateProject.MINIMAL -> MinimalTemplateFactory()
        }
        factory.createProjectFromTemplate(name, baseDir)
    }
    private enum class TemplateProject {
        PLAIN,
        MINIMAL,
    }
}
