package com.chromia.cli.integration

import java.io.File
import net.postchain.client.core.PostchainClient
import net.postchain.common.tx.TransactionStatus
import net.postchain.crypto.KeyPair
import net.postchain.rell.api.base.RellApiBaseInternal
import net.postchain.rell.api.base.RellApiBaseUtils
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliEnv
import net.postchain.rell.base.compiler.base.core.C_CompilerOptions
import net.postchain.rell.base.compiler.base.utils.C_SourceDir
import net.postchain.rell.base.lib.test.Rt_TestBlockValue
import net.postchain.rell.base.model.R_App
import net.postchain.rell.base.model.R_FunctionDefinition
import net.postchain.rell.base.model.R_Module
import net.postchain.rell.base.model.R_ModuleName
import net.postchain.rell.base.runtime.Rt_CallContext
import net.postchain.rell.base.runtime.Rt_ChainContext
import net.postchain.rell.base.runtime.Rt_LogPrinter
import net.postchain.rell.base.runtime.Rt_NullSqlContext
import net.postchain.rell.base.runtime.Rt_OutPrinter
import net.postchain.rell.base.runtime.Rt_Printer
import net.postchain.rell.base.sql.NoConnSqlManager
import net.postchain.rell.base.sql.NullSqlInitProjExt
import net.postchain.rell.base.utils.Rt_UnitTestBlockRunner
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult
import net.postchain.rell.base.utils.UnitTestMatcher
import net.postchain.rell.base.utils.UnitTestRunner
import net.postchain.rell.base.utils.UnitTestRunnerContext
import net.postchain.rell.base.utils.UnitTestRunnerResults
import net.postchain.rell.base.utils.toImmList


object RellApiRunIntegrationTests {
    /**
     * Run tests.
     *
     * Use-case 1: run tests same way as **`multirun`** does. Set [appModules] to the app's main module, add the
     * main module to [testModules], set [compileConfig.includeTestSubModules][RellApiCompile.Config.includeTestSubModules]
     * to `true`.
     *
     * Use-case 2: run all tests. Add the *root* module (`""`) to [testModules],
     * set [compileConfig.includeTestSubModules][RellApiCompile.Config.includeTestSubModules] to `true`.
     *
     * @param config Configuration.
     * @param sourceDir Source directory.
     * @param appModules List of app modules. Empty means none, `null` means all. Defines active modules for blocks
     * execution (tests can execute only operations defined in active modules).
     * @param testModules List of test modules to run. Empty means none. Can contain also app modules, if
     * [compileConfig.includeTestSubModules][RellApiCompile.Config.includeTestSubModules] is `true`.
     */
    fun runTests(
            config: Config,
            sourceDir: File,
            testModules: List<String>,
            client: PostchainClient
    ): UnitTestRunnerResults {
        val cSourceDir = C_SourceDir.diskDir(sourceDir)
        val rAppModules = listOf<R_ModuleName>()
        val rTestModules = testModules.map { R_ModuleName.of(it) }.toImmList()

        val compileConfig = config.compileConfig
        val options = RellApiBaseInternal.makeCompilerOptions(compileConfig)
        val (_, app) = RellApiBaseInternal.compileApp(compileConfig, options, cSourceDir, rAppModules, rTestModules)
        return RellApiGtxInternal.runTests(config, options, app, client)
    }

    class Config(
            /** Compilation config. */
            val compileConfig: RellApiCompile.Config,
            /** CLI environment used to print tests execution progress (test cases) and results. */
            val cliEnv: RellCliEnv,
            /** Stop tests after the first error. */
            val stopOnError: Boolean,
            /** List of glob patterns to filter test cases: when not `null`, only tests matching one of the patterns will be executed. */
            val testPatterns: List<String>?,
            /** Printer used for Rell `print()` calls. */
            val outPrinter: Rt_Printer,
            /** Printer used for Rell `log()` calls. */
            val logPrinter: Rt_Printer,
            /** Print test case names and results during the execution. */
            val printTestCases: Boolean,
            /** Add dependencies of test modules to the active modules available during block execution (default: `true`). */
            val addTestDependenciesToBlockRunModules: Boolean,
            /** Test case start callback. */
            val onTestCaseStart: (UnitTestCase) -> Unit,
            /** Test case finished callback. */
            val onTestCaseFinished: (UnitTestCaseResult) -> Unit,
    ) {
        fun toBuilder() = Builder(this)

        companion object {
            val DEFAULT = Config(
                    compileConfig = RellApiCompile.Config.DEFAULT,
                    cliEnv = RellCliEnv.DEFAULT,
                    stopOnError = false,
                    testPatterns = null,
                    outPrinter = Rt_OutPrinter,
                    logPrinter = Rt_LogPrinter(),
                    printTestCases = true,
                    addTestDependenciesToBlockRunModules = true,
                    onTestCaseStart = {},
                    onTestCaseFinished = {},
            )
        }

        class Builder(proto: Config = DEFAULT) {
            private var compileConfig = proto.compileConfig
            private var cliEnv = proto.cliEnv
            private var stopOnError = proto.stopOnError
            private var testPatterns = proto.testPatterns
            private var outPrinter = proto.outPrinter
            private var logPrinter = proto.logPrinter
            private var printTestCases = proto.printTestCases
            private var addTestDependenciesToBlockRunModules = proto.addTestDependenciesToBlockRunModules
            private var onTestCaseStart = proto.onTestCaseStart
            private var onTestCaseFinished = proto.onTestCaseFinished

            /** @see [Config.compileConfig] */
            fun compileConfig(v: RellApiCompile.Config) = apply { compileConfig = v }

            /** @see [Config.cliEnv] */
            fun cliEnv(v: RellCliEnv) = apply { cliEnv = v }

            /** @see [Config.stopOnError] */
            fun stopOnError(v: Boolean) = apply { stopOnError = v }

            /** @see [Config.testPatterns] */
            fun testPatterns(v: List<String>?) = apply { testPatterns = v?.toImmList() }

            /** @see [Config.outPrinter] */
            fun outPrinter(v: Rt_Printer) = apply { outPrinter = v }

            /** @see [Config.logPrinter] */
            fun logPrinter(v: Rt_Printer) = apply { logPrinter = v }

            /** @see [Config.printTestCases] */
            fun printTestCases(v: Boolean) = apply { printTestCases = v }

            /** @see [Config.addTestDependenciesToBlockRunModules]  */
            fun addTestDependenciesToBlockRunModules(v: Boolean) = apply { addTestDependenciesToBlockRunModules = v }

            /** @see [Config.onTestCaseStart] */
            fun onTestCaseStart(v: (UnitTestCase) -> Unit) = apply { onTestCaseStart = v }

            /** @see [Config.onTestCaseFinished] */
            fun onTestCaseFinished(v: (UnitTestCaseResult) -> Unit) = apply { onTestCaseFinished = v }

            fun build(): Config {
                return Config(
                        compileConfig = compileConfig,
                        cliEnv = cliEnv,
                        stopOnError = stopOnError,
                        testPatterns = testPatterns?.toImmList(),
                        outPrinter = outPrinter,
                        logPrinter = logPrinter,
                        printTestCases = printTestCases,
                        addTestDependenciesToBlockRunModules = addTestDependenciesToBlockRunModules,
                        onTestCaseStart = onTestCaseStart,
                        onTestCaseFinished = onTestCaseFinished,
                )
            }
        }
    }
}

object RellApiGtxInternal {
    fun runTests(
            config: RellApiRunIntegrationTests.Config,
            options: C_CompilerOptions,
            app: R_App,
            client: PostchainClient,
    ): UnitTestRunnerResults {
        val globalCtx = RellApiBaseUtils.createGlobalContext(
                options,
                typeCheck = false,
                outPrinter = config.outPrinter,
                logPrinter = config.logPrinter,
        )

        val blockRunner = IntegrationTestBlockRunner(client)

        val testMatcher = if (config.testPatterns == null) UnitTestMatcher.ANY else UnitTestMatcher.make(config.testPatterns)
        val testFns = getTestFunctions(app, testMatcher)
        val testCases = testFns.map { UnitTestCase(null, it) }

        val testCtx = UnitTestRunnerContext(
                app = app,
                printer = config.cliEnv::print,
                sqlCtx = Rt_NullSqlContext.create(app),
                sqlMgr = NoConnSqlManager,
                sqlInitProjExt = NullSqlInitProjExt,
                globalCtx = globalCtx,
                chainCtx = Rt_ChainContext.NULL,
                blockRunner = blockRunner,
                printTestCases = config.printTestCases,
                stopOnError = config.stopOnError,
                onTestCaseStart = config.onTestCaseStart,
                onTestCaseFinished = config.onTestCaseFinished,
        )

        val testRes = UnitTestRunnerResults()
        UnitTestRunner.runTests(testCtx, testCases, testRes)
        return testRes
    }

    fun getTestFunctions(app: R_App, matcher: UnitTestMatcher): List<R_FunctionDefinition> {
        val modules = app.modules
                .filter { it.test && it.selected }
                .sortedBy { it.name }

        val fns = modules.flatMap { getTestFunctions(it, matcher) }
        return fns
    }

    fun getTestFunctions(module: R_Module, matcher: UnitTestMatcher): List<R_FunctionDefinition> {
        return module.functions.values
                .filter { it.moduleLevelName == "integration_test" || it.moduleLevelName.startsWith("it_") }
                .filter { it.params().isEmpty() }
                .filter { matcher.matchFunction(it.defName) }
    }
    private class IntegrationTestBlockRunner(val client: PostchainClient) : Rt_UnitTestBlockRunner() {
        override fun runBlock(ctx: Rt_CallContext, block: Rt_TestBlockValue) {
            block.txs().forEach { tx ->
                val res = client.transactionBuilder(tx.signers.map { KeyPair.of(it.pub.toHex(), it.priv.toHex()) })
                        .apply { tx.ops.forEach { op -> addOperation(op.name.str(), *op.args.toTypedArray()) } }
                        .postAwaitConfirmation()
                require(res.status == TransactionStatus.CONFIRMED) { "Transaction with operations ${tx.ops} failed:\n$res" }
            }
        }
    }
}
