package com.chromia.cli

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import net.postchain.rell.codegen.CodeGenerator
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.document.DocumentSaver

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
        val generator = CodeGenerator(factory(), codeGeneratorConfig(), CliktCliEnv(this))
        val modules = moduleName ?: settings.model.blockchains.map { it.value.module }
        val sections = modules.flatMap { generator.createSections(settings.sourceDir, listOf(it)) }
        val documents = generator.constructDocuments(sections)
        val targetFolder = target ?: File(settings.targetDir, defaultTargetFolder)
        DocumentSaver(targetFolder).saveDocuments(documents)
        echo("Created files in $targetFolder: ${documents.keys}")
    }
}
