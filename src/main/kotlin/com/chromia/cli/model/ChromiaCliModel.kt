package com.chromia.cli.model

import net.postchain.rell.model.R_LangVersion

data class ChromiaCliModel(
        private val database: DatabaseModel = DatabaseModel(),
        private val compiler: CompilerModel = CompilerModel(),
        val blockchains: Map<String,BlockchainModel> = mapOf(),
        val deployment: Map<String, DeploymentModel> = mapOf()
) {
    init {
       // require(blockchain != null || blockchains.isNotEmpty()) { "You must have blockchain or blockchains arg" } Creates a bug with --help
        //require(!(blockchain != null && blockchains.isNotEmpty())) { "You may not have both blockchain and blockchains arg at the same time" }
    }
    val compilerOptions get() = compiler.getCompilerOptions()
    val compilerQuiet get() = compiler.quiet
    val rellVersion get() = R_LangVersion.of(compiler.rellVersion)
    val databaseUrl get() = database.url
    val databaseErrorLogging get() = database.logSqlErrors
    val databaseSchema get() = database.schema
    val databaseDriver get() = database.driver
}
