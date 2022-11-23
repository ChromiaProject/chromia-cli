package com.chromia.cli.model

data class DatabaseModel(val host: String = "localhost",
                         val database: String = "postchain",
                         val username: String = "postchain",
                         private val password: String = "postchain",
                         val logSqlErrors: Boolean = true,
                         val schema: String = "rell_app",
                         val driver: String = "org.postgresql.Driver"
                          ) {
    val url get() = "jdbc:postgresql://$host/$database?user=$username&password=$password"
}