package com.chromia.api

import com.chromia.api.impl.compileGtv
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import net.postchain.common.types.WrappedByteArray
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.codegen.CodeGeneratorConfig
import net.postchain.rell.codegen.document.DocumentFactory
import java.nio.file.Path

object ChromiaCompileApi {

    /**
     * Builds blockchain configurations
     */
    fun build(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
            : List<BlockchainConfiguration> = compileGtv(cliEnv, model, projectDir)

    /**
     * Verifies rell source code and computes the RID
     */
    @ExperimentalApi
    fun verify(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
            : WrappedByteArray = com.chromia.api.impl.verify(cliEnv, model, projectDir)
}

object ChromiaLibrariesApi {
    /**
     * Installs libraries to src/lib folder.
     */
    @ExperimentalApi("May want to remove RepositoryCloner parameter from this api")
    fun install(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, repositoryCloner: RepositoryCloner)
        = com.chromia.api.impl.install(cliEnv, repositoryCloner, projectDir, model)
}

object ChromiaGenerateApi {
    /**
     * Generates a documentation website
     */
    @JvmOverloads fun docsSite(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, targetDir: Path? = null): Unit
        = com.chromia.api.impl.docsSite(cliEnv, model, projectDir, targetDir ?:  model.compile.targetFile(projectDir.toFile()).toPath().resolve("site"))

    @ExperimentalApi
    @JvmOverloads fun generate(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path, factory: DocumentFactory, config: CodeGeneratorConfig, targetDir: Path? = null, modules: List<String>? = null) : Unit
        = com.chromia.api.impl.generate(cliEnv, model, projectDir, targetDir ?: model.compile.targetFile(projectDir.toFile()).toPath().resolve("generated"), factory, config, modules)
}
