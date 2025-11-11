package com.chromia.cli.command.generate

import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.groupSwitch
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.javascript.JavascriptCodeGeneratorConfig
import net.postchain.rell.codegen.javascript.JavascriptDocumentFactory
import net.postchain.rell.codegen.kotlin.KotlinCodeGeneratorConfig
import net.postchain.rell.codegen.kotlin.KotlinDocumentFactory
import net.postchain.rell.codegen.python.PythonCodeGeneratorConfig
import net.postchain.rell.codegen.python.PythonDocumentFactory
import net.postchain.rell.codegen.typescript.TypescriptCodeGeneratorConfig
import net.postchain.rell.codegen.typescript.TypescriptDocumentFactory

class GenerateClientStubsCommand : AbstractCodeGeneratorCommand(name = "client-stubs", help = "Generates client code for a rell dapp") {

    private val language by option(help = "Language to generate client for").groupSwitch(
        "--kotlin" to KotlinOption(),
        "--typescript" to TypescriptOption(),
        "--javascript" to JavascriptOption(),
        "--python" to PythonOption()
    )

    override val defaultTargetFolder = "stubs"

    override fun codeGeneratorConfig() = language
        ?: throw PrintMessage("Missing language option: ${SupportedLanguages.entries.map { "--" + it.alias }}", 1)

    override fun factory() = codeGeneratorConfig().factory()
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

class TypescriptOption : TypescriptCodeGeneratorConfig, LanguageOption(SupportedLanguages.TYPESCRIPT) {
    override fun factory() = TypescriptDocumentFactory()
}

class JavascriptOption : JavascriptCodeGeneratorConfig, LanguageOption(SupportedLanguages.JAVASCRIPT) {
    override fun factory() = JavascriptDocumentFactory()
}

class PythonOption : PythonCodeGeneratorConfig, LanguageOption(SupportedLanguages.PYTHON) {
    override fun factory() = PythonDocumentFactory()
}

enum class SupportedLanguages(val alias: String) {
    TYPESCRIPT("typescript"),
    JAVASCRIPT("javascript"),
    KOTLIN("kotlin"),
    PYTHON("python"),
}
