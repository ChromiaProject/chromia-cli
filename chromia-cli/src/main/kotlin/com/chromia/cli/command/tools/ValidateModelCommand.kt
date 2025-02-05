package com.chromia.cli.command.tools

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file

class ValidateModelCommand : ChromiaCommand(name = "validate-config", help = """
    Validate a chromia config file
""".trimIndent()
) {
    private val file by option("-f", "--file").file(mustExist = true, canBeFile = true, canBeDir = false).required()

    override fun run() {
        when (file.extension) {
            "yml" -> parseModel(file)
            "yaml" -> parseModel(file)
            else -> throw PrintMessage("Unsupported file format. Expected either .yml or .yaml", 1)


        }
        echo("No issues found in ${file.name}")
    }
}
