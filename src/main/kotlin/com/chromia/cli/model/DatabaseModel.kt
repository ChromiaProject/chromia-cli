package com.chromia.cli.model

data class DatabaseModel(
        private val host: String = "localhost",
        private val database: String = "postchain",
        private val username: String = "postchain",
        private val password: String = "postchain",
        private val schema: String = "rell_app",
        val driver: String = "org.postgresql.Driver",
        val logSqlErrors: Boolean = true,
) {
    val dbUrl get() =
            System.getenv("CHR_DB_URL") ?: "jdbc:postgresql://$host/$database"

    val dbUser get() = System.getenv("CHR_DB_USER") ?: username
    val dbPassword = System.getenv("CHR_DB_PASSWORD") ?: password
    val dbSchema get() = System.getenv("CHR_DB_SCHEMA") ?: schema

    val url get() = "$dbUrl?user=$dbUser&password=$dbPassword"
}
