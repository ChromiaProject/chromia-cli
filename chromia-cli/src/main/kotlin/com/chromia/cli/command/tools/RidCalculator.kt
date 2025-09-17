package com.chromia.cli.command.tools

import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.cli.command.ChromiaCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors

class RidCalculatorCommand : ChromiaCommand(name = "rid", help = """
    Computes the RID (hash) of the contents of a folder
""".trimIndent()
) {
    private val dir by option("-p", "--path").file(mustExist = true, canBeFile = false, canBeDir = true).required()

    override fun run() {
        val path = dir.toPath()
        val calculator = DirectoryHashCalculator(path)
        val rid = calculator.compute(path, DirectoryHashCalculator.RidStrategy.LIST)
        val hexValue = rid.toHex()
        echo("The RID of the content of folder :'$path' is '${TextColors.brightBlue(hexValue)}' ")
    }
}
