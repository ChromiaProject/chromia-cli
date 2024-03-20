package com.chromia.cli.model

import com.chromia.build.tools.model.ensureNamedObject
import com.chromia.build.tools.model.ensureObject
import net.pwall.json.schema.JSONSchema


data class ChromiaModel(
        private val definitions: Any = Any(),
        private val database: DatabaseModel = DatabaseModel(),
        val compile: CompileModel = CompileModel(),
        val blockchains: Map<String, BlockchainModel> = mapOf(),
        val deployments: Map<String, DeploymentModel> = mapOf(),
        val test: TestModel = TestModel(),
        val libs: Map<String, RellLibraryModel> = mapOf(),
) {
    companion object {
        fun load(data: Map<String, Any>) = ChromiaModel(
                database = ensureObject<DatabaseModel>(data["database"], DatabaseModel::load, DatabaseModel()),
                compile = ensureObject<CompileModel>(data["compile"], CompileModel::load, CompileModel()),
                blockchains = ensureNamedObject<BlockchainModel>(data["blockchains"], BlockchainModel::load),
                deployments = ensureNamedObject<DeploymentModel>(data["deployments"], DeploymentModel::load),
                test = ensureObject<TestModel>(data["test"], TestModel::load, TestModel()),
                libs = ensureNamedObject<RellLibraryModel>(data["libs"], RellLibraryModel::load),
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
