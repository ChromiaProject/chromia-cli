package com.chromia.cli.model

data class DatabaseModel(
        private val host: String = "localhost",
        private val database: String = "postchain",
        private val username: String = "postchain",
        private val password: String = "postchain",
        private val schema: String = "rell_app",
        val driver: String = "org.postgresql.Driver",
        val logSqlErrors: Boolean = false,
) {
    val dbUrl get() =
            System.getenv("CHR_DB_URL") ?: "jdbc:postgresql://$host/$database"

    val dbUser get() = System.getenv("CHR_DB_USER") ?: username
    val dbPassword = System.getenv("CHR_DB_PASSWORD") ?: password
    val dbSchema get() = System.getenv("CHR_DB_SCHEMA") ?: schema

    val url get() = "$dbUrl?user=$dbUser&password=$dbPassword"

    companion object {
        fun load(data: Map<String, Any>) = DatabaseModel(
                host = data["host"]?.let { it as String } ?: "localhost",
                database = data["database"]?.let { it as String } ?: "postchain",
                username = data["username"]?.let { it as String } ?: "postchain",
                password = data["password"]?.let { it as String } ?: "postchain",
                schema = data["schema"]?.let { it as String } ?: "rell_dapp",
                driver = data["driver"]?.let { it as String } ?: "org.postgresql.Driver",
                logSqlErrors = data["logSqlErrors"]?.let { it as Boolean } ?: false,
        )
    }
}
