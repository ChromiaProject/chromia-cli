package com.chromia.cli

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.chromiaConfigOption
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.module
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.shell.RellApiRunShell


class ReplCommand : CliktCommand(help = "Run rell commands in shell") {
    private val settings by chromiaConfigOption()
    private val sourceDir by lazy { settings.nullableModel?.compile?.sourceFile(settings.modelFile.parentFile) ?: File(System.getProperty("user.dir")) }
    private val module by module()
    private val sqlLog by option(help = "Log sql expressions").flag()
    private val historyFile by option(help = "Save command history to this file").file(canBeDir = false, mustBeWritable = true)
    private val useDB by option(help = "If a session towards the configured database should be established").flag()

    override fun run() {

        if (module != null && settings.nullableModel == null) {
            echo("To find the module \"$module\", specifying the settings file is required")
            return
        }

        if (useDB && settings.nullableModel == null) {
            throw CliktError("To correctly connect to the database, specifying the settings file is required")
        }
        val localModel = settings.nullableModel ?: ChromiaModel()
        val compileConfig = RellApiCompile.Config.Builder()
                .cliEnv(CliktCliEnv(this))
                .mountConflictError(false)
                .quiet(localModel.compile.quiet)
                .build()

        val shellConfig = RellApiRunShell.Config.Builder()
                .compileConfig(compileConfig)
                .databaseUrl(if (useDB) localModel.databaseUrl else null)
                .historyFile(historyFile)
                .sqlErrorLog(localModel.logSqlErrors)
                .sqlLog(sqlLog)
                .build()
        RellApiRunShell.runShell(shellConfig, sourceDir, module?.str())
    }
}
