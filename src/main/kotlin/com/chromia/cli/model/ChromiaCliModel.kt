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
    val logSqlErrors get() = database.logSqlErrors
    val databaseDriver get() = database.driver
    val databasePassword get() = database.dbPassword
    val databaseSchema get() = database.dbSchema
    val databaseUrl get() = database.url
    val databaseUser get() = database.dbUser
}
