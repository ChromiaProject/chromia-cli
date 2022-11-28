package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.rell.*
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.model.R_QualifiedName
import net.postchain.rell.module.GtvToRtContext
import net.postchain.rell.runtime.*
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.sql.SqlUtils.initDatabase
import net.postchain.rell.utils.PostchainUtils
import net.postchain.rell.utils.RellCliErr
import net.postchain.rell.utils.RellCliUtils

class ReplCommand: CliktCommand(help= "Run rell command in file") {
    private val config by settingsOption()
    private val sqlMapper by chainSQLMapper()
    private val wipeDB by wipeDatabaseOption()
    private val sql by sqlOption()
    private val entrypoint by entry()
    private val module by module()
    private val args by arguments()
    private val json by option("--json", help = "If a json args are used (Not yet supported)").flag()

    override fun run() {
        val globalCtx = ContextCreator().createGlobalContext(config.compilerOptions)
        //val app = RellCliUtils.compileApp(.absolutePath, module, config.compilerQuiet, C_CompilerOptions.DEFAULT)
        val (entryModule, entryRoutine) = parseEntryPoint(module, entrypoint)
        //runApp(globalCtx, entryModule, entryRoutine, app)
    }

    /*private fun runApp(
            globalCtx: Rt_GlobalContext,
            entryModule: R_ModuleName?,
            entryRoutine: R_QualifiedName?,
            app: R_App
    ) {
        val launcher = getAppLauncher(app, entryModule, entryRoutine)
        if (launcher == null && !wipeDB) { return }

        val appCtx = createRegularAppContext(globalCtx, app)
        DatabaseUtil().runWithSqlManager(config.databaseErrorLogging, config.databaseUrl) { sqlMgr ->
            val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, sqlMapper)
            if (sql) { initDatabase(appCtx,sqlCtx, sqlMgr, wipeDB, true ) }
            launcher?.launch(appCtx, sqlMgr, sqlCtx)
        }
    }
    private fun createRegularAppContext(globalCtx: Rt_GlobalContext, app: R_App): Rt_AppContext {
        val chainCtx = RellCliUtils.createChainContext()
        return Rt_AppContext(
                globalCtx,
                chainCtx,
                app,
                repl = false,
                test = false,
                replOut = null,
                blockRunnerStrategy = Rt_UnsupportedBlockRunnerStrategy
        )
    }

    private fun getAppLauncher(app: R_App, entryModule: R_ModuleName?, entryRoutine: R_QualifiedName?): RellAppLauncher? {
        if (entryModule == null || entryRoutine == null) return null
        val entryPoint = findEntryPoint(app, entryModule, entryRoutine)
        return RellAppLauncher(entryPoint, args, json)
    }

    private class RellAppLauncher(private val entryPoint: RellEntryPoint, private val args: List<String>, val json: Boolean) {
        fun launch(appCtx: Rt_AppContext, sqlMgr: SqlManager, sqlCtx: Rt_SqlContext) {
            val opCtx = entryPoint.opContext()

            val rtRes = sqlMgr.execute(entryPoint.transaction) { sqlExec ->
                val exeCtx = Rt_ExecutionContext(appCtx, opCtx, sqlCtx, sqlExec)
                val gtvCtx = GtvToRtContext(true)
                val rtArgs = parseArgs(entryPoint, gtvCtx, args, json)
                gtvCtx.finish(exeCtx)
                callEntryPoint(exeCtx, rtArgs, entryPoint)
            }


            if (rtRes != null && rtRes != Rt_UnitValue) {
                val strRes = resultToString(rtRes, json)
                println(strRes)
            }
        }

        private fun resultToString(res: Rt_Value, json: Boolean): String {
            return if (json) {
                val type = res.type()
                if (!type.completeFlags().gtv.toGtv) {
                    throw RellCliErr("Result of type '${type.strCode()}' cannot be converted to Gtv")
                }
                val gtv = type.rtToGtv(res, true)
                PostchainUtils.gtvToJson(gtv)
            } else {
                res.toString()
            }
        }
    }*/
}