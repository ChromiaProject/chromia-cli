package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.parser.findRellFilesInDir
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.common.BlockchainRid
import net.postchain.rell.compiler.base.core.C_CompilerModuleSelection
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.lib.test.Rt_DynamicBlockRunnerStrategy
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_FunctionDefinition
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.runtime.utils.Rt_SqlManager
import net.postchain.rell.sql.NoConnSqlManager
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.utils.*
import kotlin.system.exitProcess

class TestCommand: CliktCommand(help= "Run tests in working directory") {
    private val sourceDir by sourceDirOption()
    private val modules by modulesFiles()
    private val blockchainRid by bridOption()
    private val sql by sqlOption()
    private val sqlMapper by chainSQLMapper()
    private val settings by settingsOption()


    override fun run() {
        runMultiModuleTest()
    }
    private fun runMultiModuleTest() {
        val sourceDir = C_SourceDir.diskDir(sourceDir)
        val modSel = C_CompilerModuleSelection(settings.test.modules.map {  R_ModuleName.Companion.of(it) })
        val app = RellCliUtils.compileApp(sourceDir, modSel, settings.compilerQuiet, C_CompilerOptions.DEFAULT)
        val testFns = TestRunner.getTestFunctions(app, TestMatcher.ANY)
        runTests(app, testFns, sourceDir)
    }

    private fun runTests(app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        if (sql) {
            DatabaseUtil().runWithSqlManager(settings.databaseErrorLogging, settings.databaseUrl) { sqlManager ->
                runner(sqlManager, app, fns, sourceDir)
            }
        } else {
            runner(Rt_SqlManager(NoConnSqlManager, settings.databaseErrorLogging ), app, fns, sourceDir)
        }
    }
    private fun runner(sqlManager: SqlManager, app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        val context = ContextCreator().createTestContext(app,
                settings.compilerOptions,
                blockchainRid ?: BlockchainRid(ByteArray(32)),
                sqlMapper,
                settings.test.moduleArgs)
        val blockRunnerModules = app.modules.filter { !it.test && !it.abstract && !it.external }.map { it.name }
        val blockRunnerStrategy = Rt_DynamicBlockRunnerStrategy(sourceDir, blockRunnerModules, context.keyPair)
        val testCtx = TestRunnerContext(context.sqlCtx, sqlManager, context.globalCtx, context.chainCtx, blockRunnerStrategy, app)
        val cases = fns.map { TestRunnerCase(null, it) }
        TestRunner.runTests(testCtx, cases)
    }
}
