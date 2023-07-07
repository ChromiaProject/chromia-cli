package com.chromia.cli

import com.chromia.cli.tools.config.chromiaConfigOption
import com.chromia.cli.tools.formatter.danger
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.tools.formatter.success
import com.chromia.cli.tools.formatter.warning
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.modulesOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import net.postchain.gtv.Gtv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.Rt_Printer
import net.postchain.rell.base.runtime.utils.Rt_Utils
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult
import net.postchain.rell.base.utils.UnitTestRunnerResults


class TestCommand : CliktCommand(help = "Run tests in working directory") {

    private val blockchains by blockchainOption(help = "Select which blockchain(s) to test", metavar = "BLOCKCHAIN")
            .multiple()
    private val modules by modulesOption()
    private val settings by chromiaConfigOption()
    private val tests by option(help = "test method pattern").split(",")
    private val sourceDir by lazy { settings.sourceDir }
    private val useDB by option(help = "If a session towards the configured database should be established")
            .flag("--no-db", default = true)

    override fun run() {
        try {
            if (shouldRunBlockchainTests()) {
                runBlockchainTests()
            }
            if (shouldRunUnitTests()) {
                runUnitTests()
            }
        } catch (e: RellCliException) {
            throw CliktError(e.message)
        }
    }

    private fun runBlockchainTests() {
        settings.model.blockchains
                .filter { it.value.test.modules.isNotEmpty() }
                .filter { blockchains.isEmpty() || blockchains.contains(it.key) }
                .forEach { runTestsForChain(it.key) }
    }

    private fun runUnitTests() {
        val testModules = modules ?: settings.testModel.modules
        val testModuleArgs = settings.testModel.moduleArgs
        val testConf = createTestConfig(testModuleArgs)

        currentContext.terminal.println("=".repeat(20) + "Running unit tests" + "=".repeat(20))
        val res = RellApiRunTests.runTests(testConf, sourceDir, listOf(), testModules)
        printResults(res)
    }

    private fun shouldRunUnitTests() = blockchains.isEmpty() || modules != null

    private fun shouldRunBlockchainTests() = modules == null || blockchains.isNotEmpty()

    private fun runTestsForChain(blockchain: String) {
        val chainConfig = settings.model.blockchains[blockchain]
                ?: throw CliktError("Blockchain '$blockchain' not found")

        val appModules = listOf(chainConfig.module)
        val testModules = chainConfig.test.modules
        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, appModuleInTestsError = true)

        echo("Running tests for chain: $blockchain")
        val res = RellApiRunTests.runTests(testConf, sourceDir, appModules, testModules)
        printResults(res)
    }

    private fun createTestConfig(testModuleArgs: Map<String, Map<String, Gtv>>,
                                 appModuleInTestsError: Boolean = false): RellApiRunTests.Config {
        val printer = object : Rt_Printer {
            override fun print(str: String) = echo(str)
        }
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(testModuleArgs)
                .cliEnv(CliktCliEnv(this))
                .includeTestSubModules(true)
                .appModuleInTestsError(appModuleInTestsError)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(settings.compileModel.langVersion)
                .quiet(settings.compileModel.quiet)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .testPatterns(tests)
                .databaseUrl(if (useDB) settings.model.databaseUrl else null)
                .stopOnError(settings.testModel.failOnError)
                .sqlErrorLog(settings.model.logSqlErrors)
                .logPrinter(printer)
                .outPrinter(printer)
                .printTestCases(false)
                .onTestCaseStart { case -> case.print() }
                .onTestCaseFinished { res -> res.print() }
                .build()
    }

    private fun mergeModuleArgs(first: Map<String, Map<String, Gtv>>,
                                second: Map<String, Map<String, Gtv>>): Map<String, Map<String, Gtv>> {
        return (first.asSequence() + second.asSequence())
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) ->
                    values.flatMap { map -> map.entries }.associate(Map.Entry<String, Gtv>::toPair)
                }
    }

    private fun UnitTestCase.print() {

        echo("${info("TEST")}: $name")
    }

    private fun UnitTestCaseResult.print() {
        if (res.isOk) {
            echo("${success(res.toString())}: $case")
        } else {
            echo("${warning(res.toString())}: $case")
        }
    }

    private fun printResults(results: UnitTestRunnerResults) {

        val (okTests, failedTests) = results.getResults().partition { it.res.error == null }

        if (failedTests.isNotEmpty()) {
            echo()
            echo("-".repeat(60))
            echo("FAILED TESTS:")
            for (r in failedTests) {
                echo()
                echo(r.case.name)
                printException(r.res.error!!)
            }
        }

        echo()
        echo("-".repeat(60))
        echo("TEST RESULTS:")

        printResults(okTests, success)
        printResults(failedTests, danger)

        val nTests = results.getResults().size
        val nOk = okTests.size
        val nFailed = failedTests.size

        echo("\nSUMMARY: $nFailed FAILED / $nOk PASSED / $nTests TOTAL\n")

        if (nFailed == 0) {
            currentContext.terminal.success("***** OK *****")
        } else {
            throw CliktError(TextColors.red("***** FAILED *****"))
        }
    }

    private fun printResults(list: List<UnitTestCaseResult>, color: TextStyle) {
        if (list.isNotEmpty()) {
            echo()
            for (r in list) {
                echo("${color(r.res.toString())} ${r.case}")
            }
        }
    }

    private fun printException(e: Throwable) {
        when (e) {
            is Rt_Exception -> {
                val msg = Rt_Utils.appendStackTrace("${TextColors.red("ERROR:")} ${e.message}", e.info.stack)
                echo(msg)
            }

            else -> {
                echo(e.stackTraceToString())
            }
        }
    }
}
