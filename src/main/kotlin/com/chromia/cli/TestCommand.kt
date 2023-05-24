package com.chromia.cli

import com.chromia.cli.util.AnsiColorScheme
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.ColorAware
import com.chromia.cli.util.ColorFormat
import com.chromia.cli.util.NoColorScheme
import com.chromia.cli.util.green
import com.chromia.cli.util.heading
import com.chromia.cli.util.line
import com.chromia.cli.util.blockchainsOption
import com.chromia.cli.util.modulesOption
import com.chromia.cli.util.red
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.space
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
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


class TestCommand : CliktCommand(help = "Run tests in working directory"), ColorAware {

    private val blockchains by blockchainsOption()
    private val modules by modulesOption()
    private val settings by settingsOption()
    private val tests by option(help = "test method pattern").split(",")
    private val sourceDir by lazy { settings.source }
    private val useDB by option(help = "If a session towards the configured database should be established")
            .flag("--no-db", default = true)
    override val colorScheme by option("--no-color", help = "Do not use ansi colors").flag()
            .convert { if (it) NoColorScheme(::echo) else AnsiColorScheme(::echo) }

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }


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
        settings.blockchains
                .filter { it.value.test.modules.isNotEmpty() }
                .filter { blockchains.isEmpty() || blockchains.contains(it.key) }
                .forEach { runTestsForChain(it.key) }
    }

    private fun runUnitTests() {
        val testModules = modules ?: settings.test.modules
        val testModuleArgs = settings.test.moduleArgs
        val testConf = createTestConfig(testModuleArgs)

        heading("Running unit tests")
        val res = RellApiRunTests.runTests(testConf, sourceDir, listOf(), testModules)
        printResults(res)
    }

    private fun shouldRunUnitTests() = blockchains.isEmpty() || modules != null

    private fun shouldRunBlockchainTests() = modules == null || blockchains.isNotEmpty()

    private fun runTestsForChain(blockchain: String) {
        val chainConfig = settings.blockchains[blockchain]
                ?: throw CliktError("Blockchain '$blockchain' not found")

        val appModules = listOf(chainConfig.module)
        val testModules = chainConfig.test.modules
        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, appModuleInTestsError = true)

        heading("Running tests for chain: $blockchain")
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
                .version(settings.compile.langVersion)
                .quiet(settings.compile.quiet)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .testPatterns(tests)
                .databaseUrl(if (useDB) settings.model.databaseUrl else null)
                .stopOnError(settings.test.failOnError)
                .sqlErrorLog(settings.model.logSqlErrors)
                .logPrinter(printer)
                .outPrinter(printer)
                .printTestCases(false)
                .onTestCaseStart { case -> case.print() }
                .onTestCaseFinished { res -> res.print() }
                .build()
    }

    private fun mergeModuleArgs(first: Map<String, Map<String,Gtv>>,
                                second: Map<String, Map<String,Gtv>>): Map<String, Map<String,Gtv>> {
        return (first.asSequence() + second.asSequence())
                .groupBy({ it.key }, { it.value })
                .mapValues { (_, values) ->
                    values.flatMap { map -> map.entries }.associate(Map.Entry<String, Gtv>::toPair)
                }
    }

    private fun UnitTestCase.print() {
        echo("${colorScheme.blue.format("TEST")}: $name")
    }

    private fun UnitTestCaseResult.print() {
        if (res.isOk) {
            green("$res $case")
        } else {
            red("$res $case")
        }
    }

    private fun printResults(results: UnitTestRunnerResults) {

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
            throw CliktError(colorScheme.red.format("***** FAILED *****"))
        }
    }

    private fun printResults(list: List<UnitTestCaseResult>, color: ColorFormat) {
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
}
