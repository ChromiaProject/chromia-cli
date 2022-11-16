package com.example.rell

import java.util.*

class DatabaseProperties(properties: Properties) {
    var host: String = properties["host"].toString()
    var database: String = properties["database"].toString()
    var username: String = properties["username"].toString()
    var password: String = properties["password"].toString()

    fun getConnectionUrl (): String {
        return String.format("jdbc:postgresql://%s/%s?user=%s&password=%s", host, database, username, password)
    }
}