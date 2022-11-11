package com.example.rell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.StorageBuilder
import net.postchain.common.BlockchainRid
import net.postchain.config.app.AppConfig
import net.postchain.gtv.GtvNull
import net.postchain.rell.compiler.base.core.C_AtAttrShadowing
import net.postchain.rell.compiler.base.core.C_CompilerModuleSelection
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.lib.test.Rt_DynamicBlockRunnerStrategy
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.*
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.rell.runtime.*
import net.postchain.rell.runtime.utils.Rt_SqlManager
import net.postchain.rell.sql.*
import net.postchain.rell.utils.*
import java.io.File
import java.sql.DriverManager
import kotlin.system.exitProcess

private fun  CliktCommand.sourceDirOption() =
    option("-r", "--root", help = "Root dir for project")
        .file(mustExist = true)
        .default(File(System.getProperty("user.dir")))
private fun CliktCommand.modulesFiles() =
    option("-tm","--test-modules", help = "Comma separated list of file names under the module, ex: testFile1,testFile2,...")
        .convert { R_ModuleName.of(it) }.split(",")
private fun  CliktCommand.rellVersion() =
    option("-v", "--version", help = "Target version of rell")
        .default("0.10.10")
private fun CliktCommand.brid() =
    option("-brid", "--blockchain-rid", help = "Blockchain RID")
        .convert { BlockchainRid.buildFromHex(it) }
        .default(BlockchainRid(ByteArray(32)))

private fun CliktCommand.sql() =
    option("-db", "--database", help = "If a database is used ").flag()


class RunTest: CliktCommand(help= "Run tests in working directory") {
    private val sourceFolder by sourceDirOption()
    private val testRootModules by modulesFiles()
    private val rellVersion by rellVersion()
    private val blockchainRid by brid()
    private val sql by sql()




    override fun run() {

        if (testRootModules == null) {
            throw RellCliErr("Must specify test-modules argument")
        } else {
            runMultiModuleTest(testRootModules!!)
        }
        exitProcess(0)
    }
    private fun runMultiModuleTest(modules: List<R_ModuleName>) {
        println(modules)
        val sourceDir = C_SourceDir.diskDir(sourceFolder)
        val modSel = C_CompilerModuleSelection(modules)
        val app = RellCliUtils.compileApp(sourceDir, modSel, false, C_CompilerOptions.DEFAULT)
        val testFns = TestRunner.getTestFunctions(app, TestMatcher.ANY)
        if (sql) {
            runTests(app, testFns, sourceDir)
        }else {
            runTests(app, testFns, sourceDir)
        }

    }

    private fun runWithSqlManager(logSqlErrors: Boolean, code: (SqlManager) -> Unit) {
        val dbUrl = "jdbc:postgresql://localhost/postchain?user=postchain&password=postchain"
        val dbProperties = ""
        val sqlLogging = true
        val schema = SqlUtils.extractDatabaseSchema(dbUrl)
        DriverManager.getConnection(dbUrl).use { con ->
            con.autoCommit = true
            val sqlMgr = ConnectionSqlManager(con, sqlLogging)
            runWithSqlManager(schema, sqlMgr, logSqlErrors, code)
        }

    }

    private fun runWithSqlManager(
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



    private fun runTests(app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        val context = createContext(app);
        val blockRunnerModules = app.modules.filter { !it.test && !it.abstract && !it.external }.map { it.name }
        val blockRunnerStrategy = Rt_DynamicBlockRunnerStrategy(sourceDir, blockRunnerModules, context.keyPair)

        if (sql) {
            println("in db")
            runWithSqlManager(true) { sqlManager ->
                val testCtx = TestRunnerContext(context.sqlCtx, sqlManager, context.globalCtx, context.chainCtx, blockRunnerStrategy, app)
                val cases = fns.map { TestRunnerCase(null, it) }
                TestRunner.runTests(testCtx, cases)
            }
        } else {
                val sqlManager = Rt_SqlManager(NoConnSqlManager, logErrors = true )
                val testCtx = TestRunnerContext(context.sqlCtx, sqlManager, context.globalCtx, context.chainCtx, blockRunnerStrategy, app)
                val cases = fns.map { TestRunnerCase(null, it) }
                TestRunner.runTests(testCtx, cases)
        }

    }
    data class Context(
        var globalCtx: Rt_GlobalContext,
        var chainCtx: Rt_ChainContext,
        val sqlCtx: Rt_SqlContext,
        val keyPair: BytesKeyPair
    )
    private fun createContext(app: R_App): Context {
        val SQL_MAPPER = Rt_ChainSqlMapping(100) //TODO create a config for
        val compilerOptions = C_CompilerOptions(R_LangVersion.of(rellVersion),
            gtv = false,
            deprecatedError = true,
            ide = false,
            blockCheck =false,
            C_AtAttrShadowing.DEFAULT,
            testLib = true,
            hiddenLib = false,
            allowDbModificationsInObjectExprs = false,
            symbolInfoFile = null
        )

        val globalCtx = Rt_GlobalContext(compilerOptions,Rt_OutPrinter,Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
        val chainCtx = Rt_ChainContext(GtvNull, immMapOf(), blockchainRid)

        val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, SQL_MAPPER)

        val keyPair = UnitTestBlockRunner.getTestKeyPair() //this is static in the test scope Bob and alice

        return Context(globalCtx, chainCtx, sqlCtx, keyPair)
    }
}
