package com.chromia.cli.model

data class ChromiaCliModel(
        private val definitions: Any = Any(), // Placeholder for anchor objects
        private val database: DatabaseModel = DatabaseModel(),
        val compile: CompileModel = CompileModel(),
        val blockchains: Map<String, BlockchainModel> = mapOf(),
        val deployments: Map<String, DeploymentModel> = mapOf(),
        val test: TestModel = TestModel(),
        val libs: Map<String, RellLibraryModel> = mapOf(),
) {
    val compilerOptions get() = compile.getCompilerOptions()
    val compilerQuiet get() = compile.quiet
    val databaseDriver get() = database.driver
    val databaseErrorLogging get() = database.logSqlErrors
    val databasePassword get() = database.dbPassword
    val databaseSchema get() = database.dbSchema
    val databaseUrl get() = database.url
    val databaseUser get() = database.dbUser
}
