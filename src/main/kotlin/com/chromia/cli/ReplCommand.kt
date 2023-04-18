package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.util.module
import com.chromia.cli.util.settingsOptionNotRequired
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.repl.ReplShell
import net.postchain.rell.sql.NoConnSqlManager
import net.postchain.rell.sql.SqlManager
import org.postgresql.util.PSQLException
import java.io.File


class ReplCommand : CliktCommand(help = "Run rell commands in shell") {
    private val settings by settingsOptionNotRequired()
    private val sourceDir by lazy { settings?.source ?: File(System.getProperty("user.dir")) }
    private val module by module()
    private val useDB by option(help = "If a session towards the configured database should be established").flag()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
//TODO add check that if there are libs, that they are installed
        if (module != null && settings == null) {
            echo("To find the module \"$module\", specifying the settings file is required")
            return
        }

        if (useDB && settings == null) {
            throw CliktError("To correctly connect to the database, specifying the settings file is required")
        }

        val localModel = settings?.model ?: ChromiaCliModel()

        if (useDB) {
            try {
                DatabaseUtil.runWithSqlManager(localModel.databaseErrorLogging, localModel.databaseUrl) { sqlManager ->
                    startShell(sqlManager, localModel)
                }
            } catch (e: PSQLException) {
                echo(e.message)
            }
        } else {
            startShell(NoConnSqlManager, localModel)
        }
    }

    private fun startShell(manager: SqlManager, model: ChromiaCliModel) {
        ReplShell.start(C_SourceDir.diskDir(sourceDir),
                module,
                ContextCreator.createGlobalContext(model.compile.getCompilerOptions()),
                manager,
                useDB,
                model.compile.getCompilerOptions())
    }
}