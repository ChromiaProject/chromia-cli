package com.chromia.cli

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.codegen.CodeGenerator
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.MermaidCodeGeneratorConfig
import net.postchain.rell.codegen.MermaidDocumentFactory
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.document.DocumentSaver
import net.postchain.rell.codegen.javascript.JavascriptCodeGeneratorConfig
import net.postchain.rell.codegen.javascript.JavascriptDocumentFactory
import net.postchain.rell.codegen.kotlin.KotlinCodeGeneratorConfig
import net.postchain.rell.codegen.kotlin.KotlinDocumentFactory
import net.postchain.rell.codegen.typescript.TypescriptCodeGeneratorConfig
import net.postchain.rell.codegen.typescript.TypescriptDocumentFactory
import java.io.File

class GenerateClientStubsCommand : CliktCommand(name = "generate-client-stubs", help = "Generates client code for a rell dapp") {
    private val settings by chromiaModelOption()

    private val moduleName by option("-m", "--module",
            help = "Explicitly set which modules to generate code for. Separate modules with ','").split(",")

    private val language by option(help = "Language to generate for").groupSwitch(
            "--kotlin" to KotlinOption(),
            "--mermaid" to MermaidOption(),
            "--typescript" to TypescriptOption(),
            "--javascript" to JavascriptOption(),
    )

    private val target by option("-d", "--target", help = "Directory to generate template project in")
            .file(canBeFile = false)

    override fun run() {
        require(language != null) { "Missing language option: ${SupportedLanguages.values().map { "--" + it.alias }}" }
        val generator = CodeGenerator(language!!.factory(), language!!, CliktCliEnv(this))
        val modules = moduleName ?: settings.model.blockchains.map { it.value.module }
        val sections = modules.flatMap { generator.createSections(settings.sourceDir, listOf(it)) }
        val documents = generator.constructDocuments(sections)
        val targetFolder = target ?: File(settings.targetDir, "stubs")
        DocumentSaver(targetFolder).saveDocuments(documents)
        echo("Created files in $targetFolder: ${documents.keys}")
    }
}

sealed class LanguageOption(language: SupportedLanguages) : CodeGeneratorConfig, OptionGroup(
        name = language.alias,
        help = "Options for language ${language.alias}"
) {
    abstract fun factory(): DocumentFactory
}

class KotlinOption : KotlinCodeGeneratorConfig, LanguageOption(SupportedLanguages.KOTLIN) {
    private val packageName by option("--package", help = "Name of package").required()
    override fun packageName() = packageName
    override fun factory() = KotlinDocumentFactory(this)
}

class MermaidOption : MermaidCodeGeneratorConfig, LanguageOption(SupportedLanguages.MERMAID) {
    private val mdx by option(help = "Surround with mdx tags").flag()
    private val er by option("--entity-relation", help = "Presented as entity relation diagram or class diagram").flag("--class-diagram", default = true)
    override fun mdx() = mdx
    override fun erDiagram() = er
    override fun factory() = MermaidDocumentFactory(this)
}

class TypescriptOption : TypescriptCodeGeneratorConfig, LanguageOption(SupportedLanguages.TYPESCRIPT) {
    override fun factory() = TypescriptDocumentFactory()
}

class JavascriptOption : JavascriptCodeGeneratorConfig, LanguageOption(SupportedLanguages.JAVASCRIPT) {
    override fun factory() = JavascriptDocumentFactory()
}

enum class SupportedLanguages(val alias: String) {
    TYPESCRIPT("typescript"),
    JAVASCRIPT("javascript"),
    MERMAID("mermaid"),
    KOTLIN("kotlin"),
}
