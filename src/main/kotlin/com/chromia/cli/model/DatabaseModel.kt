package com.chromia.cli.model

data class DatabaseModel(
        val host: String = System.getenv("CHR_DB_HOST") ?: "localhost",
        val database: String = System.getenv("CHR_DB") ?: "postchain",
        val username: String = System.getenv("CHR_DB_USER") ?: "postchain",
        private val password: String = System.getenv("CHR_DB_PASSWORD") ?: "postchain",
        val schema: String = System.getenv("CHR_DB_SCHEMA") ?: "rell_app",
        val driver: String = "org.postgresql.Driver",
        val logSqlErrors: Boolean = true,
) {
    val url get() = "jdbc:postgresql://$host/$database?user=$username&password=$password"
}
