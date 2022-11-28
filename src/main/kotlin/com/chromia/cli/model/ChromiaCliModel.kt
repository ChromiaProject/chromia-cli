package com.chromia.cli.model

import net.postchain.rell.model.R_LangVersion

data class ChromiaCliModel(
        private val definitions: Any = Any(), // Placeholder for anchor objects
        private val database: DatabaseModel = DatabaseModel(),
        val compile: CompileModel = CompileModel(),
        val blockchains: Map<String, BlockchainModel> = mapOf(),
        val deployment: Map<String, DeploymentModel> = mapOf(),
) {
    val compilerOptions get() = compile.getCompilerOptions()
    val compilerQuiet get() = compile.quiet
    val rellVersion get() = R_LangVersion.of(compile.rellVersion)
    val databaseUrl get() = database.url
    val databaseErrorLogging get() = database.logSqlErrors
    val databaseSchema get() = database.schema
    val databaseDriver get() = database.driver
}
