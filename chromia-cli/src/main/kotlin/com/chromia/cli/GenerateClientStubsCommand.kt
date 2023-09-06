package com.chromia.cli

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.util.LanguageSupport
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import net.postchain.rell.codegen.CodeGenerator
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.document.DocumentSaver
import net.postchain.rell.codegen.javascript.JavascriptDocumentFactory
import net.postchain.rell.codegen.kotlin.KotlinDocumentFactory
import net.postchain.rell.codegen.typescript.TypescriptDocumentFactory

class GenerateClientStubsCommand : CliktCommand(name = "generate-client-stubs", help = "Generates client code for a rell dapp") {
    private val settings by chromiaModelOption()

    private val moduleName by option("--module",
            help = "Explicitly set which modules to generate code for. Separate modules with ','").split(",")

    private val languageOption: LanguageOption by option(help = "Language to generate for")
            .groupSwitch(
                    KotlinOption().createOption(),
                    TypescriptOption().createOption(),
                    JavascriptOption().createOption()
            ).required()

    private val target by option("--target", help = "Directory to generate template project in")
            .file(canBeFile = false)

    override fun run() {
        val generator = CodeGenerator(languageOption.factory())
        val modules = moduleName ?: settings.model.blockchains.map { it.value.module }
        val sections = modules.flatMap { generator.createSections(settings.sourceDir, listOf(it)) }
        val documents = generator.constructDocuments(sections, true)
        val targetFolder = target ?: File(settings.targetDir, "stubs")
        DocumentSaver(targetFolder).saveDocuments(documents)
        echo("Created files in $targetFolder: ${documents.keys}")
    }
}

sealed class LanguageOption(private val enum: LanguageSupport) : OptionGroup(
        name = enum.name,
        help = "Options for language ${enum.name}") {
    fun createOption() = enum.flag() to this
    abstract fun factory(): DocumentFactory
}

class KotlinOption : LanguageOption(LanguageSupport.Kotlin) {
    private val packageName by option("--package", help = "Name of package").required()
    override fun factory() = KotlinDocumentFactory(packageName)
}

class TypescriptOption : LanguageOption(LanguageSupport.Typescript) {
    override fun factory() = TypescriptDocumentFactory()
}

class JavascriptOption : LanguageOption(LanguageSupport.Javascript) {
    override fun factory() = JavascriptDocumentFactory()
}
