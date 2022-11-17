package com.chromia.cli.database

class DatabaseOptions(var host: String,
                         var database: String,
                         var username: String,
                         var password: String,
                         var dbProperties: String) {

    fun getConnectionUrl (): String {
        return "jdbc:postgresql://$host/$database?user=$username&password=$password"
    }
}