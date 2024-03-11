package com.chromia.cli.model

import net.pwall.json.schema.JSONSchema

data class ChromiaModel(
        private val database: DatabaseModel = DatabaseModel(),
        val compile: CompileModel = CompileModel(),
        val blockchains: Map<String, BlockchainModel> = mapOf(),
        val deployments: Map<String, DeploymentModel> = mapOf(),
        val test: TestModel = TestModel(),
        val libs: Map<String, RellLibraryModel> = mapOf(),
) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        fun load(data: Map<String, Any>) = ChromiaModel(
                database = data["database"]?.let { DatabaseModel.load(it as Map<String, Any>) } ?: DatabaseModel(),
                compile = data["compile"]?.let { CompileModel.load(it as Map<String, Any>) } ?: CompileModel(),
                blockchains = (data["blockchains"] as Map<String, Any>?)?.mapValues { BlockchainModel.load(it.value as Map<String, Any>, it.key) }
                        ?: mapOf(),
                deployments = (data["deployments"] as Map<String, Any>?)?.mapValues { DeploymentModel.load(it.value as Map<String, Any>, it.key) }
                        ?: mapOf(),
                test = data["test"]?.let { TestModel.load(it as Map<String, Any>) } ?: TestModel(),
                libs = (data["libs"] as Map<String, Any>?)?.mapValues { RellLibraryModel.load(it.value as Map<String, Any>, it.key) }
                        ?: mapOf(),
        )

        val schema = JSONSchema.parse(
                this::class.java.getResourceAsStream("/chromia-model-schema.json")!!.readAllBytes().toString(Charsets.UTF_8)
        )
    }

    val logSqlErrors get() = database.logSqlErrors
    val databaseDriver get() = database.driver
    val databasePassword get() = database.dbPassword
    val databaseSchema get() = database.dbSchema
    val databaseUrl get() = database.url
    val databaseUser get() = database.dbUser
}
