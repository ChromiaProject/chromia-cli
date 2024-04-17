package com.chromia.api.config

import com.chromia.api.ChromiaGenerateApi
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.MermaidCodeGeneratorConfig
import net.postchain.rell.codegen.MermaidDocumentFactory
import net.postchain.rell.codegen.document.DocumentFactory
import net.postchain.rell.codegen.javascript.JavascriptCodeGeneratorConfig
import net.postchain.rell.codegen.javascript.JavascriptDocumentFactory
import net.postchain.rell.codegen.kotlin.KotlinCodeGeneratorConfig
import net.postchain.rell.codegen.kotlin.KotlinDocumentFactory
import net.postchain.rell.codegen.typescript.TypescriptCodeGeneratorConfig
import net.postchain.rell.codegen.typescript.TypescriptDocumentFactory
import java.nio.file.Path

fun ChromiaGenerateApi.graph(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
    = GenerateGraphBuilder(cliEnv, model, projectDir)
fun ChromiaGenerateApi.kotlinStubs(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, packageName: String)
        = KotlinClientStubsBuilder(cliEnv, model, projectDir, packageName)
fun ChromiaGenerateApi.typescriptStubs(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
        = TypescriptClientStubsBuilder(cliEnv, model, projectDir)
fun ChromiaGenerateApi.javascriptStubs(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
        = JavascriptClientStubsBuilder(cliEnv, model, projectDir)

class GenerateGraphBuilder(val cliEnv: RellCliEnv, val model: ChromiaModel, val projectDir: Path) {

    var targetDir: Path = model.compile.targetFile(projectDir.toFile()).toPath().resolve("graph")
    var erDiagram: Boolean = true
    var mdx: Boolean = false

    fun setTargetDir(value: Path) = apply { targetDir = value }
    fun setIsErDiagram(value: Boolean) = apply { erDiagram = value }
    fun setIsMdx(value: Boolean) = apply { mdx = value }

    fun generate() {
        val config = object : MermaidCodeGeneratorConfig {
            override fun erDiagram() = erDiagram
            override fun mdx() = mdx
        }
        val factory = MermaidDocumentFactory(config)
        ChromiaGenerateApi.generate(cliEnv, model, projectDir, factory, config, targetDir)
    }
}

class KotlinClientStubsBuilder(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, val packageName: String): GenerateClientStubsBuilder(cliEnv, model, projectDir) {
    override val config: KotlinCodeGeneratorConfig
        get() = object : KotlinCodeGeneratorConfig {
            override fun packageName() = packageName
        }
    override val factory: DocumentFactory get() = KotlinDocumentFactory(config)
}
class TypescriptClientStubsBuilder(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path): GenerateClientStubsBuilder(cliEnv, model, projectDir) {
    override val config: TypescriptCodeGeneratorConfig
        get() = object : TypescriptCodeGeneratorConfig {}
    override val factory: DocumentFactory get() = TypescriptDocumentFactory()
}

class JavascriptClientStubsBuilder(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path): GenerateClientStubsBuilder(cliEnv, model, projectDir) {
    override val config: JavascriptCodeGeneratorConfig
        get() = object : JavascriptCodeGeneratorConfig {}
    override val factory: DocumentFactory get() = JavascriptDocumentFactory()
}

abstract class GenerateClientStubsBuilder(val cliEnv: RellCliEnv, val model: ChromiaModel, val projectDir: Path) {

    abstract val factory: DocumentFactory
    abstract val config: CodeGeneratorConfig
    var targetDir: Path = model.compile.targetFile(projectDir.toFile()).toPath().resolve("stubs")

    fun setTargetDir(value: Path) = apply { targetDir = value }

    fun generate() {
        ChromiaGenerateApi.generate(cliEnv, model, projectDir, factory, config, targetDir)
    }
}
