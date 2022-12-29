package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.util.module
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.repl.ReplShell
import net.postchain.rell.sql.NoConnSqlManager
import net.postchain.rell.sql.SqlManager
import org.postgresql.util.PSQLException

class ReplCommand: CliktCommand(help= "Run rell commands in shell") {
    private val settings by settingsOption()
    private val sourceDir by lazy { settings.source }
    private val module by module().required()
    private val useSQL by option(help = "If a session towards the configured database should be established").flag()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }
    override fun run() {

        if (useSQL) {
            try {
                DatabaseUtil.runWithSqlManager(settings.model.databaseErrorLogging, settings.model.databaseUrl) { sqlManager ->
                    startShell(sqlManager)
                }
            } catch (e: PSQLException) {
                echo(e.message)
            }
        } else {
            startShell(NoConnSqlManager)
        }
    }

    private fun startShell(manager: SqlManager) {
        ReplShell.start(C_SourceDir.diskDir(sourceDir),
                module,
                ContextCreator.createGlobalContext(settings.compile.getCompilerOptions()),
                manager,
                useSQL,
                settings.compile.getCompilerOptions())
    }
}