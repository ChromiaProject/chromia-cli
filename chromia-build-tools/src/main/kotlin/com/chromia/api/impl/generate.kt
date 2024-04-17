package com.chromia.api.impl

import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.codegen.CodeGenerator
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.document.DocumentSaver
import java.nio.file.Path

fun generate(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, targetDir: Path, factory: DocumentFactory, config: CodeGeneratorConfig, modules: List<String>?) {
    val generator = CodeGenerator(factory, config, cliEnv)
    val modulesToGenerate = modules ?: model.blockchains.map { it.value.module }
    val sections = modulesToGenerate.flatMap { generator.createSections(model.compile.sourceFile(projectDir.toFile()), listOf(it)) }
    val documents = generator.constructDocuments(sections)
    DocumentSaver(targetDir.toFile()).saveDocuments(documents)
    cliEnv.print("Created files in $targetDir: ${documents.keys}")
}