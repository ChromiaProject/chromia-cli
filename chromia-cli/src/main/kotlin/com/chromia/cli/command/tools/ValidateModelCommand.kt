package com.chromia.cli.command.tools

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.OutputFormat
import com.chromia.cli.util.formatJson
import com.chromia.cli.util.formatRaw
import com.chromia.cli.util.formatXml
import com.chromia.cli.util.formatYaml
import com.chromia.cli.util.outputFormat
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvException
import net.postchain.gtv.pretty

class ValidateModelCommand : ChromiaCommand(help = """
    Validate a chromia config file
""".trimIndent()
) {
    private val file by option("-f", "--file").file(mustExist = true, canBeFile = true, canBeDir = false).required()

    override fun run() {
        when (file.extension) {
            "yml" -> parseModel(file)
            "yaml" -> parseModel(file)
            else -> throw PrintMessage("Unsupported file format. Expected either .yml or.yaml", 1)


        }
        echo("No issues found in file ${file.name}")
    }
}
