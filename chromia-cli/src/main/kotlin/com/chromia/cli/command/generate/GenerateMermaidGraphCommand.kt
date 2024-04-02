package com.chromia.cli.command.generate

import com.chromia.cli.command.generate.AbstractCodeGeneratorCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.rell.codegen.MermaidCodeGeneratorConfig
import net.postchain.rell.codegen.MermaidDocumentFactory

class GenerateMermaidGraphCommand: AbstractCodeGeneratorCommand("graph", help = "Generates entity relation graphs in mermaid format") {
    private val mdx by option(help = "Surround with mdx tags").flag()
    private val er by option("--entity-relation", help = "Presented as entity relation diagram or class diagram").flag("--class-diagram", default = true)
    override fun codeGeneratorConfig() = object : MermaidCodeGeneratorConfig {
        override fun erDiagram() = er
        override fun mdx() = mdx
    }

    override fun factory() = MermaidDocumentFactory(codeGeneratorConfig())

    override val defaultTargetFolder = "mermaid"
}
