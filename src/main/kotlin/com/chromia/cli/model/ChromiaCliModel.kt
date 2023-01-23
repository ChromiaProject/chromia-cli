package com.chromia.cli.model

import net.postchain.rell.model.R_LangVersion

data class ChromiaCliModel(
        private val definitions: Any = Any(), // Placeholder for anchor objects
        private val database: DatabaseModel = DatabaseModel(),
        val compile: CompileModel = CompileModel(),
        val blockchains: Map<String, BlockchainModel> = mapOf(),
        val deployments: Map<String, DeploymentModel> = mapOf(),
        val test: TestModel = TestModel()
) {
    val compilerOptions get() = compile.getCompilerOptions()
    val compilerQuiet get() = compile.quiet
    val rellVersion get() = R_LangVersion.of(compile.rellVersion)
    val databaseDriver get() = database.driver
    val databaseErrorLogging get() = database.logSqlErrors
    val databasePassword get() = database.dbPassword
    val databaseSchema get() = database.dbSchema
    val databaseUrl get() = database.url
    val databaseUser get() = database.dbUser
}
