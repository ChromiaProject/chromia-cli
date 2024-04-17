package com.chromia.cli.command.generate

import com.chromia.api.ChromiaGenerateApi
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory

abstract class AbstractCodeGeneratorCommand(name: String, help: String): CliktCommand(name = name, help = help) {
    private val settings by chromiaModelOption()
    private val moduleName by option("-m", "--module",
            help = "Explicitly set which modules to generate code for. Separate modules with ','").split(",")
    private val target by option("-d", "--target", help = "Directory to generate code in")
            .file(canBeFile = false)

    abstract fun codeGeneratorConfig(): CodeGeneratorConfig
    abstract fun factory(): DocumentFactory
    abstract val defaultTargetFolder: String

    final override fun run() {
        ChromiaGenerateApi.generate(
                CliktCliEnv(this),
                settings.model,
                settings.projectFolder.toPath(),
                factory(),
                codeGeneratorConfig(),
                target?.toPath(),
                moduleName
        )
    }
}
