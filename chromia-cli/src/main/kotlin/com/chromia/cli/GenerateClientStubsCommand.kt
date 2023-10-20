package com.chromia.cli

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.LanguageSupport
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
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

    private val moduleName by option("--module",
            help = "Explicitly set which modules to generate code for. Separate modules with ','").split(",")

    private val language by option(help = "Langage to generate for").groupSwitch(
            "--kotlin" to KotlinOption(),
            "--mermaid" to MermaidOption(),
            "--typescript" to TypescriptOption(),
            "--javascript" to JavascriptOption(),
    ).required()

    private val target by option("--target", help = "Directory to generate template project in")
            .file(canBeFile = false)

    override fun run() {
        val factory = when (language) {
            is KotlinOption -> KotlinDocumentFactory(language as KotlinCodeGeneratorConfig)
            is JavascriptOption -> JavascriptDocumentFactory()
            is TypescriptOption -> TypescriptDocumentFactory()
            is MermaidOption -> MermaidDocumentFactory(language as MermaidCodeGeneratorConfig)
        }
        val generator = CodeGenerator(factory, language)
        val modules = moduleName ?: settings.model.blockchains.map { it.value.module }
        val sections = modules.flatMap { generator.createSections(settings.sourceDir, listOf(it)) }
        val documents = generator.constructDocuments(sections)
        val targetFolder = target ?: File(settings.targetDir, "stubs")
        DocumentSaver(targetFolder).saveDocuments(documents)
        echo("Created files in $targetFolder: ${documents.keys}")
    }
}

sealed class LanguageOption(language: String) : CodeGeneratorConfig, OptionGroup(language) {
}

class KotlinOption : KotlinCodeGeneratorConfig, LanguageOption(LanguageSupport.Kotlin.name) {
    private val packageName by option("--package", help = "Name of package").required()
    override fun packageName() = packageName
}

class MermaidOption : MermaidCodeGeneratorConfig, LanguageOption("Mermaid") {
    private val mdx by option(help = "Surround with mdx tags").flag()
    override fun mdx() = mdx
}

class TypescriptOption : TypescriptCodeGeneratorConfig, LanguageOption(LanguageSupport.Typescript.name) {
}

class JavascriptOption : JavascriptCodeGeneratorConfig, LanguageOption(LanguageSupport.Javascript.name) {
}
