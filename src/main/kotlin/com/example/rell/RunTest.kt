package com.example.rell

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvNull
import net.postchain.rell.*
import net.postchain.rell.compiler.base.core.C_AtAttrShadowing
import net.postchain.rell.compiler.base.core.C_CompilerModuleSelection
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.lib.test.Rt_DynamicBlockRunnerStrategy
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_FunctionDefinition
import net.postchain.rell.model.R_LangVersion
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.rell.runtime.*
import net.postchain.rell.runtime.utils.Rt_SqlManager
import net.postchain.rell.sql.NoConnSqlManager
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.utils.*
import java.io.File
import kotlin.system.exitProcess


class RunTest: CliktCommand(help="Run tests in target directory") {

    val sourceFolder by option("--source").file(mustExist = true).default(File("${System.getProperty("user.dir")}/sample/"))
    val testModule by option("--testmodule").convert { R_ModuleName.of(it) }.split(",").default(listOf(R_ModuleName.of("testfiles.SimpleTests")))
    val mainModule by option("--mainmodule").convert { R_ModuleName.of(it) }.split(",").default(listOf(R_ModuleName.of("testfiles")))
    val rellVersion by option("--version").default("0.10.10")

    override fun run() {

        val sourceDir = C_SourceDir.diskDir(sourceFolder)
        val modSel = C_CompilerModuleSelection(mainModule, testModule)
        val app = RellCliUtils.compileApp(sourceDir, modSel, false, C_CompilerOptions.DEFAULT)

        val testFns = TestRunner.getTestFunctions(app, TestMatcher.ANY)
        runTests(app, testFns)

        exitProcess(0)
    }

private fun runTests(app: R_App, fns: List<R_FunctionDefinition>) {
    val compilerOptions = C_CompilerOptions(R_LangVersion.of(rellVersion), gtv = false,deprecatedError = true, ide = false,blockCheck =false,
        C_AtAttrShadowing.DEFAULT,testLib = true,hiddenLib = false,allowDbModificationsInObjectExprs = false, symbolInfoFile = null)
    val globalCtx = Rt_GlobalContext(compilerOptions,Rt_OutPrinter,Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
    val chainCtx = Rt_ChainContext(GtvNull, immMapOf(), BlockchainRid(ByteArray(32))) // to be a variable the brid

    val SQL_MAPPER = Rt_ChainSqlMapping(100)
    val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, SQL_MAPPER)

    val sourceDir = C_SourceDir.diskDir(sourceFolder) //TODO refactor
    val keyPair = UnitTestBlockRunner.getTestKeyPair() //this is static in the test scope Bob and alice

    val blockRunnerModules = app.modules.filter { !it.test && !it.abstract && !it.external }.map { it.name }
    val blockRunnerStrategy = Rt_DynamicBlockRunnerStrategy(sourceDir, blockRunnerModules, keyPair)


    val manager = NoConnSqlManager
    val sqlManager = Rt_SqlManager(manager, logErrors = true )
    //TODO FIX SO WE CAN WRITE TO DB WITH Rt_SqlManager with a conmanager
    val testCtx = TestRunnerContext(sqlCtx, sqlManager, globalCtx, chainCtx, blockRunnerStrategy, app)
    val cases = fns.map { TestRunnerCase(null, it) }
    TestRunner.runTests(testCtx, cases)
}
}
