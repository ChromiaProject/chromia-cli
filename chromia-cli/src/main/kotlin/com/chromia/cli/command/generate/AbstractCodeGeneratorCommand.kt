package com.chromia.cli.command.generate

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.targetDirectoryOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import net.postchain.rell.codegen.CodeGenerator
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.document.DocumentSaver
import java.io.File

abstract class AbstractCodeGeneratorCommand(name: String, help: String): ChromiaCommand(name = name, help = help) {
    private val settings by chromiaModelOption()
    private val moduleName by option("-m", "--module",
            help = "Explicitly set which modules to generate code for. Separate modules with ','").split(",")
    private val target by targetDirectoryOption(help = "Directory to generate code in")
    private val hideLibWarnings by hideLibWarningsOption()

    abstract fun codeGeneratorConfig(): CodeGeneratorConfig
    abstract fun factory(): DocumentFactory
    abstract val defaultTargetFolder: String

    final override fun run() {
        val cliEnv = BuildCliEnv(this, hideLibWarnings)
        val generator = CodeGenerator(factory(), codeGeneratorConfig(), cliEnv)
        val modules = moduleName ?: settings.model.blockchains.map {
            it.value.module ?: throw CliktError("Cannot run tests for blockchain ${it.key} without main module")
        }
        val sections = modules.flatMap { generator.createSections(settings.sourceDir, listOf(it)) }
        val documents = generator.constructDocuments(sections)
        val targetFolder = target ?: File(settings.targetDir, defaultTargetFolder)
        DocumentSaver(targetFolder).saveDocuments(documents)
        echo("Created files in $targetFolder: ${documents.keys}")
        if (cliEnv.hasError) {
            throw CliktError()
        }
    }
}
