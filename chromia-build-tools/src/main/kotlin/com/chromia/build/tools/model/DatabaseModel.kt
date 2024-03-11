package com.chromia.cli.model

import com.chromia.build.tools.model.ensureType

data class DatabaseModel(
        private val host: String = "localhost",
        private val database: String = "postchain",
        private val username: String = "postchain",
        private val password: String = "postchain",
        private val schema: String = "rell_app",
        val driver: String = "org.postgresql.Driver",
        val logSqlErrors: Boolean = false,
) {
    val dbUrl
        get() =
            System.getenv("CHR_DB_URL") ?: "jdbc:postgresql://$host/$database"

    val dbUser get() = System.getenv("CHR_DB_USER") ?: username
    val dbPassword = System.getenv("CHR_DB_PASSWORD") ?: password
    val dbSchema get() = System.getenv("CHR_DB_SCHEMA") ?: schema

    val url get() = "$dbUrl?user=$dbUser&password=$dbPassword"

    companion object {
        fun load(data: Map<String, Any>) = DatabaseModel(
                host = ensureType<String?>(data["host"], "database", "host") ?: "localhost",
                database = ensureType<String?>(data["database"], "database", "database") ?: "postchain",
                username = ensureType<String?>(data["username"], "database", "username") ?: "postchain",
                password = ensureType<String?>(data["password"], "database", "password") ?: "postchain",
                schema = ensureType<String?>(data["schema"], "database", "schema") ?: "rell_dapp",
                driver = ensureType<String?>(data["driver"], "database", "driver") ?: "org.postgresql.Driver",
                logSqlErrors = ensureType<Boolean?>(data["logSqlErrors"], "database", "logSqlErrors") ?: false,
        )
    }
}
