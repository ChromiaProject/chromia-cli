package com.chromia.cli

import com.chromia.cli.compile.ContextCreator
import com.chromia.cli.database.DatabaseUtil
import com.chromia.cli.util.AnsiColor
import com.chromia.cli.util.AnsiColorScheme
import com.chromia.cli.util.ColorAware
import com.chromia.cli.util.ColorFormat
import com.chromia.cli.util.ColorScheme
import com.chromia.cli.util.NoColorScheme
import com.chromia.cli.util.green
import com.chromia.cli.util.line
import com.chromia.cli.util.modulesOption
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.space
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.rell.compiler.base.core.C_CompilerModuleSelection
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_FunctionDefinition
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.runtime.Rt_BlockRunnerStrategy
import net.postchain.rell.runtime.Rt_ChainSqlMapping
import net.postchain.rell.runtime.Rt_Exception
import net.postchain.rell.runtime.utils.Rt_Utils
import net.postchain.rell.sql.NoConnSqlManager
import net.postchain.rell.sql.SqlManager
import net.postchain.rell.tools.runcfg.RunConfigGtvBuilder
import net.postchain.rell.utils.RellCliEnv
import net.postchain.rell.utils.RellCliUtils
import net.postchain.rell.utils.TestCaseResult
import net.postchain.rell.utils.TestMatcher
import net.postchain.rell.utils.TestRunner
import net.postchain.rell.utils.TestRunnerCase
import net.postchain.rell.utils.TestRunnerContext
import net.postchain.rell.utils.TestRunnerResults

class TestCommand : CliktCommand(help = "Run tests in working directory"), ColorAware {
    private val modules by modulesOption()
    private val settings by settingsOption()
    private val tests by option(help = "test method pattern")
    private val sourceDir by lazy { settings.source }
    private val useDB by option(help = "If a session towards the configured database should be established").flag()
    override val colorScheme by option("-B", "--batch-mode").flag()
            .convert { if (it) NoColorScheme(::echo) else AnsiColorScheme(::echo) }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        val testModules = modules ?: settings.test.modules.map { R_ModuleName.Companion.of(it) }
        val sourceDir = C_SourceDir.diskDir(sourceDir)
        val modSel = C_CompilerModuleSelection(listOf(), testModules)
        val app = RellCliUtils.compileApp(sourceDir, modSel, settings.model.compilerQuiet, C_CompilerOptions.DEFAULT)
        val testFns = TestRunner.getTestFunctions(app, tests?.let { TestMatcher.make(it) } ?: TestMatcher.ANY)
        runTests(app, testFns, sourceDir)
    }

    private fun runTests(app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        if (useDB) {
            DatabaseUtil.runWithSqlManager(settings.model.databaseErrorLogging, settings.model.databaseUrl) { sqlManager ->
                runner(sqlManager, app, fns, sourceDir)
            }
        } else {
            runner(NoConnSqlManager, app, fns, sourceDir)
        }
    }

    private fun runner(sqlManager: SqlManager, app: R_App, fns: List<R_FunctionDefinition>, sourceDir: C_SourceDir) {
        val context = ContextCreator.createTestContext(app,
                settings.model.compilerOptions,
                BlockchainRid(ByteArray(32)),
                Rt_ChainSqlMapping(100),
                settings.test.moduleArgs)
        val blockRunnerModules = app.modules.filter { !it.test && !it.abstract && !it.external }.map { it.name }
        val blockRunnerStrategy = TestBlockRunnerStrategy(sourceDir, blockRunnerModules, settings.test.moduleArgs)
        val testCtx = TestRunnerContext(context.sqlCtx, sqlManager, context.globalCtx, context.chainCtx, blockRunnerStrategy, app)
        val cases = fns.map { TestRunnerCase(null, it) }
        val results = TestRunnerResults()
        TestRunner.runTests(testCtx, cases, results)
        printResults(results)
    }

    private fun printResults(results: TestRunnerResults) {

        val (okTests, failedTests) = results.getResults().partition { it.res.error == null }

        if (failedTests.isNotEmpty()) {
            space()
            line()
            echo("FAILED TESTS:")
            for (r in failedTests) {
                space()
                echo(r.case.name)
                printException(r.res.error!!)
            }
        }

        space()
        line()
        echo("TEST RESULTS:")

        printResults(okTests, colorScheme.green)
        printResults(failedTests, colorScheme.red)

        val nTests = results.getResults().size
        val nOk = okTests.size
        val nFailed = failedTests.size

        echo("\nSUMMARY: $nFailed FAILED / $nOk PASSED / $nTests TOTAL\n")

        if (nFailed == 0) {
            green("***** OK *****")
        } else {
            throw CliktError(AnsiColor.Red.format("***** FAILED *****"))
        }
    }

    private fun printResults(list: List<TestCaseResult>, color: ColorFormat) {
        if (list.isNotEmpty()) {
            space()
            for (r in list) {
                echo("${color.format(r.res)} ${r.case}")
            }
        }
    }

    private fun printException(e: Throwable) {
        when (e) {
            is Rt_Exception -> {
                val msg = Rt_Utils.appendStackTrace("${colorScheme.red.format("ERROR:")} ${e.message}", e.info.stack)
                echo(msg)
            }

            else -> {
                echo(e.stackTraceToString())
            }
        }
    }

    private inner class TestBlockRunnerStrategy(val sourceDir: C_SourceDir, val modules: List<R_ModuleName>, val moduleArgs: Map<String, Map<String, Gtv>>) : Rt_BlockRunnerStrategy() {

        override fun createGtvConfig(): Gtv {
            return UnitTestBlockRunner.makeGtvConfig(BlockRunnerRellCliEnv(), sourceDir, modules, getKeyPair().pub).let { config ->
                RunConfigGtvBuilder().apply {
                    update(config)
                    update(gtv(moduleArgs.mapValues { gtv(it.value) }), "gtx", "rell", "moduleArgs")
                }.build()
            }
        }

        override fun getKeyPair() = UnitTestBlockRunner.getTestKeyPair()

        private inner class BlockRunnerRellCliEnv : RellCliEnv() {
            override fun print(msg: String, err: Boolean) {
                echo(msg)
            }

            override fun exit(status: Int): Nothing {
                throw Rt_Exception.common("block_runner", "Gtv config generation failed")
            }
        }
    }
}
