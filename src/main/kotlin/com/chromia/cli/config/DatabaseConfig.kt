package com.chromia.cli.config

data class DatabaseConfig(val host: String = "localhost",
                          val database: String = "postchain",
                          val username: String = "postchain",
                          private val password: String = "postchain",
                          val logSqlErrors: Boolean = true
                          ) {
    val url get() = "jdbc:postgresql://$host/$database?user=$username&password=$password"
}