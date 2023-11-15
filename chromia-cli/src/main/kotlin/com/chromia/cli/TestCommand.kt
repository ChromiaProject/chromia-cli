package com.chromia.cli

import com.chromia.build.tools.test.xmlTestReport
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.tools.formatter.danger
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.tools.formatter.success
import com.chromia.cli.tools.formatter.warning
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.logSqlOption
import com.chromia.cli.util.modulesOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyle
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import net.postchain.gtv.Gtv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.utils.Rt_Utils
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult
import net.postchain.rell.base.utils.UnitTestResult
import net.postchain.rell.base.utils.UnitTestRunnerResults


class TestCommand : CliktCommand(help = "Run tests in working directory") {

    private val blockchains by blockchainOption(help = "Select which blockchain(s) to test", metavar = "BLOCKCHAIN")
            .multiple()
    private val modules by modulesOption()
    private val settings by chromiaModelOption()
    private val sqlLog by logSqlOption()
    private val tests by option(help = "test method pattern").split(",")
    private val sourceDir by lazy { settings.sourceDir }
    private val useDB by option(help = "If a session towards the configured database should be established")
            .flag("--no-db", default = true)
    private val testReport by option(help = "Generate JUnit XML test reports")
            .flag()
    private val testReportDir by option(help = "JUnit XML test reports directory (defaults to \"build/reports\")")
            .file(canBeDir = true, canBeFile = false)

    override fun run() {
        val testReportPath = testReportDir ?: File(settings.targetDir, "reports")
        if (testReport) {
            testReportPath.mkdirs()
        }
        try {
            if (shouldRunBlockchainTests()) {
                runBlockchainTests(testReportPath.toPath())
            }
            if (shouldRunUnitTests()) {
                runUnitTests(testReportPath.toPath())
            }
        } catch (e: RellCliException) {
            throw CliktError(e.message)
        }
    }

    private fun runBlockchainTests(testReportPath: Path) {
        settings.model.blockchains
                .filter { it.value.test.modules.isNotEmpty() }
                .filter { blockchains.isEmpty() || blockchains.contains(it.key) }
                .forEach { runTestsForChain(it.key, testReportPath) }
    }

    private fun runUnitTests(testReportPath: Path) {
        val testModules = modules ?: settings.model.test.modules
        val testModuleArgs = settings.model.test.moduleArgs
        val testConf = createTestConfig(testModuleArgs)

        currentContext.terminal.println("=".repeat(20) + "Running unit tests" + "=".repeat(20))
        val res = RellApiRunTests.runTests(testConf, sourceDir, listOf(), testModules)
        if (testReport) {
            Files.writeString(testReportPath.resolve("rell-unit-tests.xml"), res.xmlTestReport("rell"))
        }
        printResults(res)
    }

    private fun shouldRunUnitTests() = blockchains.isEmpty() || modules != null

    private fun shouldRunBlockchainTests() = modules == null || blockchains.isNotEmpty()

    private fun runTestsForChain(blockchain: String, testReportPath: Path) {
        val chainConfig = settings.model.blockchains[blockchain]
                ?: throw CliktError("Blockchain '$blockchain' not found")

        val appModules = listOf(chainConfig.module)
        val testModules = chainConfig.test.modules
        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, appModuleInTestsError = true)

        echo("Running tests for chain: $blockchain")
        val res = RellApiRunTests.runTests(testConf, sourceDir, appModules, testModules)
        if (testReport) {
            Files.writeString(testReportPath.resolve("${blockchain}-tests.xml"), res.xmlTestReport(blockchain))
        }
        printResults(res)
    }

    private fun createTestConfig(testModuleArgs: Map<String, Map<String, Gtv>>,
                                 appModuleInTestsError: Boolean = false): RellApiRunTests.Config {
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(testModuleArgs)
                .cliEnv(CliktCliEnv(this))
                .includeTestSubModules(true)
                .appModuleInTestsError(appModuleInTestsError)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(settings.model.compile.langVersion)
                .quiet(settings.model.compile.quiet)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .testPatterns(tests)
                .databaseUrl(if (useDB) "${settings.model.databaseUrl}&currentSchema=${settings.model.databaseSchema}_tests" else null)
                .stopOnError(settings.model.test.failOnError)
                .sqlErrorLog(settings.model.logSqlErrors)
                .logPrinter(::echo)
                .outPrinter(::echo)
                .printTestCases(false)
                .onTestCaseStart { case -> case.print() }
                .onTestCaseFinished { res -> res.print() }
                .sqlLog(sqlLog)
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
            echo("${success(res.toString())}: $case (${UnitTestResult.durationToString(res.duration)})")
        } else {
            echo("${warning(res.toString())}: $case (${UnitTestResult.durationToString(res.duration)})")
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
        val duration = results.getResults().fold(Duration.ZERO) { acc, res -> acc.plus(res.res.duration) }

        echo("\nSUMMARY: $nFailed FAILED / $nOk PASSED / $nTests TOTAL (${UnitTestResult.durationToString(duration)})\n")

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
