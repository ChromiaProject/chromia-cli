package com.chromia.cli.command.tools

import com.chromia.build.tools.lib.DirectoryHashCalculator
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.parseModel
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path

class GenerateLibModelCommand : ChromiaCommand(name = "lib-model", help = """
    Generates a library model template for users to add to their chromia model as dependency.
""".trimIndent()
) {
    private val libSource by option("-s", "--library-source").path(mustExist = true, canBeFile = false, canBeDir = true).required()

    override fun run() {
        val calculator = DirectoryHashCalculator(libSource)
        val rid = calculator.compute(libSource, DirectoryHashCalculator.RidStrategy.LIST)

        echo(
                """
                libs:
                    <Name of the library>:
                        registry: <git reference>
                        path: $libSource
                        tagOrBranch: <Tag or branch the library is published on>
                        rid: ${rid.toHex()}
                        insecure: false
                 
                """.trimIndent()
        )
    }
}