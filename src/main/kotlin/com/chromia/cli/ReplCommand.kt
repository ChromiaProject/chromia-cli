package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.compile.ContextCreator.createRegularAppContext
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.rell.*
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.BlockchainRid
import net.postchain.rell.compiler.base.core.C_CompilerModuleSelection
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.model.R_App
import net.postchain.rell.module.GtvToRtContext
import net.postchain.rell.runtime.*
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.sql.SqlUtils.initDatabase
import net.postchain.rell.utils.RellCliUtils

class ReplCommand: CliktCommand(help= "Run rell command in file") {
    private val settings by settingsOption()
    private val sourceDir by lazy { settings.compile.source }
    private val wipeDB by wipeDatabaseOption()
    private val entrypoint by entry()
    private val module by module().required()
    private val args by arguments()
    //private val json by option("--json", help = "If a json args are used").flag()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }
    override fun run() {
        val modSel = C_CompilerModuleSelection(listOf(module))
        val sourceDir = C_SourceDir.diskDir(sourceDir)
        val app = RellCliUtils.compileApp(sourceDir, modSel, settings.compilerQuiet, C_CompilerOptions.DEFAULT)
        val context = ContextCreator.createTestContext(app,
                settings.compilerOptions,
                BlockchainRid(ByteArray(32)),
                Rt_ChainSqlMapping(100),
                mapOf()
        )
        runApp(context, app)
    }

    private fun runApp(context: ContextCreator.Context, app: R_App) {
        val appCtx = createRegularAppContext(context.globalCtx, app)
        DatabaseUtil.runWithSqlManager(settings.databaseErrorLogging, settings.databaseUrl) { sqlManager ->
            initDatabase(appCtx,context.sqlCtx, sqlManager, wipeDB, false )
            entryPoint(app)?.let { launch(appCtx, sqlManager, context.sqlCtx, entryPoint = it) }
        }
    }

    private fun entryPoint(app: R_App): RellEntryPoint? {
        val (entryModule, entryRoutine) = parseEntryPoint(module, entrypoint)
        if (entryModule == null || entryRoutine == null) return null
        return findEntryPoint(app, entryModule, entryRoutine)
    }

    private fun launch(appCtx: Rt_AppContext, sqlMgr: SqlManager, sqlCtx: Rt_SqlContext, entryPoint: RellEntryPoint) {
        val rtRes = sqlMgr.execute(entryPoint.transaction) { sqlExec ->
            val exeCtx = Rt_ExecutionContext(appCtx, entryPoint.opContext(), sqlCtx, sqlExec)
            val gtvCtx = GtvToRtContext.make(true)
            val rtArgs = parseArgs(entryPoint, gtvCtx, args, false)
            gtvCtx.finish(exeCtx)
            callEntryPoint(exeCtx, rtArgs, entryPoint)
        }

        if (rtRes != null && rtRes != Rt_UnitValue) {
            println(rtRes)
        }
    }
}