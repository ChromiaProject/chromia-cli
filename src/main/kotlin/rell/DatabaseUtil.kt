package com.example.rell

import net.postchain.rell.runtime.utils.Rt_SqlManager
import net.postchain.rell.sql.ConnectionSqlManager
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.sql.SqlUtils
import java.sql.DriverManager

class DatabaseUtil {

    fun runWithSqlManager(logSqlErrors: Boolean, databaseProperties: DatabaseProperties, code: (SqlManager) -> Unit) {
        println(databaseProperties.getConnectionUrl())
        val dbUrl = databaseProperties.getConnectionUrl()
        val dbProperties = ""
        val sqlLogging = true
        val schema = SqlUtils.extractDatabaseSchema(dbUrl)
        DriverManager.getConnection(dbUrl).use { con ->
            con.autoCommit = true
            val sqlMgr = ConnectionSqlManager(con, sqlLogging)
            runWithSqlManager(schema, sqlMgr, logSqlErrors, code)
        }

    }

    fun runWithSqlManager(
        schema: String?,
        sqlMgr: SqlManager,
        logSqlErrors: Boolean,
        code: (SqlManager) -> Unit
    ) {
        val sqlMgr2 = Rt_SqlManager(sqlMgr, logSqlErrors)
        if (schema != null) {
            sqlMgr2.transaction { sqlExec ->
                sqlExec.connection { con ->
                    SqlUtils.prepareSchema(con, schema)
                }
            }
        }
        code(sqlMgr2)
    }
}