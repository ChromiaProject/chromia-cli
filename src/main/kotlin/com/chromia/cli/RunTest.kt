package com.chromia.cli.compile

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvNull
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
import java.util.*
import kotlin.system.exitProcess

private fun  CliktCommand.sourceDirOption() =
        option(help = "Rell source directory") // Name is implicit from variable name
            .file(mustExist = true, canBeFile = false, canBeDir = true)
            .default(File(System.getProperty("user.dir")))

private fun CliktCommand.modulesFiles() =
        option("-tm","--test-modules", help = "Comma separated list of file names under the module, ex: testFile1,testFile2,...")
        .convert { R_ModuleName.of(it) }.split(",")
private fun CliktCommand.brid() =
        option("-brid", "--blockchain-rid", help = "Blockchain RID")
        .convert { BlockchainRid.buildFromHex(it) }
        .default(BlockchainRid(ByteArray(32)))

private fun CliktCommand.sql() =
        option("-db", "--database", help = "If a database is used ").flag()

private fun CliktCommand.databaseOption() =
        option("-p", "--db-properties", help = "File path with database settings" )
        .default("database.properties")

private fun CliktCommand.settingsOption() =
        option("-s", "--settings", help = "Alternate path for the settings file" )
                .file(mustExist = true, canBeFile = true, canBeDir = false)
                .default(File("compilerOptions.properties"))


private fun CliktCommand.chainSQLMapper() =
        option("-cid", "--chainid", help = "Chainid, defaults to 100" )
        .long()
        .convert { Rt_ChainSqlMapping(it) }
        .default(Rt_ChainSqlMapping(100))

class TestCommand: CliktCommand(help= "Run tests in working directory") {
    private val sourceFolder by sourceDirOption()
    private val testRootModules by modulesFiles()
    private val blockchainRid by brid()
    private val sql by sql()
    private val dbProperties by databaseOption()
    private val coProperties by settingsOption()
    private val sqlMapper by chainSQLMapper()

    private var databaseProperties: DatabaseProperties? = null

    override fun run() {
        val properties = Properties()
        val stream = this::class.java.classLoader.getResourceAsStream(dbProperties)
        if (stream == null) {
            throw RellCliErr("Must specify a resource file for database properties")
        } else {
            properties.load(stream)
            databaseProperties = DatabaseProperties(properties)
        }

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

    private fun runTests(app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        val context = createContext(app);
        val blockRunnerModules = app.modules.filter { !it.test && !it.abstract && !it.external }.map { it.name }
        val blockRunnerStrategy = Rt_DynamicBlockRunnerStrategy(sourceDir, blockRunnerModules, context.keyPair)

        if (sql) {
            databaseProperties?.let {
                DatabaseUtil().runWithSqlManager(true, it) { sqlManager ->
                    val testCtx = TestRunnerContext(context.sqlCtx, sqlManager, context.globalCtx, context.chainCtx, blockRunnerStrategy, app)
                    val cases = fns.map { TestRunnerCase(null, it) }
                    TestRunner.runTests(testCtx, cases)
                }
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
        val properties = Properties()
        println(coProperties.absolutePath)
        properties.load(coProperties.inputStream())
        val compilerOptions = CompilerOptions(properties).getCompilerOptions()
        val globalCtx = Rt_GlobalContext(compilerOptions,Rt_OutPrinter,Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
        val chainCtx = Rt_ChainContext(GtvNull, immMapOf(), blockchainRid)
        val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, sqlMapper)
        val keyPair = UnitTestBlockRunner.getTestKeyPair() //this is static in the test scope Bob and alice
        return Context(globalCtx, chainCtx, sqlCtx, keyPair)

    }
}
