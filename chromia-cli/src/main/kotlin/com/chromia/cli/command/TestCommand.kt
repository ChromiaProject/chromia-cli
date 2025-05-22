package com.chromia.cli.command

import com.chromia.build.tools.test.xmlTestReport
import com.chromia.cli.coverage.CodeCoverageCollector
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.sql.SqlStatisticsCollector
import com.chromia.cli.sql.SqlStatisticsRenderer
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.tools.formatter.danger
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.tools.formatter.success
import com.chromia.cli.tools.formatter.warning
import com.chromia.cli.util.addWhitelistedGTXModules
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.modulesOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValueLazy
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.rendering.TextStyle
import com.github.ajalt.mordant.terminal.success
import net.postchain.gtv.Gtv
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.base.RellCliException
import net.postchain.rell.api.gtx.RellApiRunTests
import net.postchain.rell.base.utils.UnitTestCase
import net.postchain.rell.base.utils.UnitTestCaseResult
import net.postchain.rell.base.utils.UnitTestResult
import net.postchain.rell.base.utils.UnitTestRunnerResults
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalTime
import java.time.format.DateTimeFormatter

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS ")

enum class SqlLoggingType {
    USER,
    SYSTEM,
    BOTH,
    NONE;
}

class TestCommand : ChromiaCommand(help = "Run tests in working directory") {

    private val blockchains by blockchainOption(help = "Run tests for specified blockchain(s). Can only be a single chain if used together with -m", metavar = "BLOCKCHAIN")
            .multiple()
    private val modules by modulesOption("Run tests in this module(s) only. Must be the part of the specified modules or its submodules. Comma delimited, will default to all modules either under each selected blockchain or test")
    private val settings by chromiaModelOption()

    private val sqlLogType by option("--sql-log", help = "Enable SQL query logging. Optional value: 'user' (only user queries) or 'system' (only system queries). Without value, logs both types.")
        .choice(
            "user" to SqlLoggingType.USER,
            "system" to SqlLoggingType.SYSTEM,
        )
        .optionalValueLazy { SqlLoggingType.BOTH }
        .default(SqlLoggingType.NONE)

    private val tests by option(help = "test method pattern", metavar = "").split(",")
    private val sourceDir by lazy { settings.sourceDir }
    private val useDB by option(help = "If a session towards the configured database should be established")
            .flag("--no-db", default = true)
    private val testReport by option(help = "Generate JUnit XML test reports")
            .flag()
    private val testReportDir by option(help = "JUnit XML test reports directory (defaults to \"build/reports\")")
            .file(canBeDir = true, canBeFile = false)

    private val failOnError by option(help = "Sets test execution to stop on error and override any \"failOnError\" settings for tests that are in the scope being executed")
            .boolean()
            .optionalValueLazy { true }

    private val coverage by option(help = "Enable code coverage")
            .boolean()
            .optionalValueLazy { true }

    private val timestamp by option("-ts", "--timestamp", help = "Timestamp on logs").flag()

    private val sqlStatisticsCollector by lazy { SqlStatisticsCollector() }
    private val sqlStatisticsRenderer by lazy { SqlStatisticsRenderer(this, sqlLogType) }
    private val codeCoverageCollector by lazy { CodeCoverageCollector() }

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
        validateSingleModuleSelected()
        blockchains.forEach { validateContainsModule(it) }

        settings.model.blockchains
                .filter { it.value.test.modules.isNotEmpty() }
                .filter { blockchains.isEmpty() || blockchains.contains(it.key) }
                .forEach {
                    runTestsForBlockchain(it.key, testReportPath)
                }
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
        if (sqlLogType != SqlLoggingType.NONE) {
            sqlStatisticsRenderer.display(sqlStatisticsCollector.getStatistics())
        }
        printResults(res)
        codeCoverageCollector.writeReport(testReportPath.resolve("coverage.cobertura.xml "))
    }

    private fun shouldRunUnitTests() = blockchains.isEmpty()

    private fun shouldRunBlockchainTests() = modules == null || (modules!!.isNotEmpty() && blockchains.isNotEmpty())

    private fun validateSingleModuleSelected() {
        if (blockchains.size > 1 && !modules.isNullOrEmpty()) {
            throw PrintMessage("Only one blockchain is allowed when specifying module", statusCode = 1)
        }
    }

    private fun validateContainsModule(blockchain: String) {
        val chainConfig = getBlockchainConfig(blockchain)
        if (!modules.isNullOrEmpty()) {
            modules!!.forEach { m ->
                if (!chainConfig.test.modules.any { bcm -> m.startsWith(bcm) })
                    throw PrintMessage("Test module \"$m\" is not defined under blockchain \"$blockchain\"", statusCode = 1)
            }
        }
    }

    private fun getBlockchainConfig(blockchain: String): BlockchainModel {
        return settings.model.blockchains[blockchain]
                ?: throw CliktError("Blockchain '$blockchain' not found")
    }

    private fun runTestsForBlockchain(blockchain: String, testReportPath: Path) {
        val chainConfig = getBlockchainConfig(blockchain)

        val appModules = listOf(chainConfig.module
                ?: throw CliktError("Cannot run tests for blockchain $blockchain without main module"))
        val testModules = modules ?: chainConfig.test.modules
        val additionalModules = chainConfig.config["gtx"]?.get("modules")?.asArray()?.map { it.asString() }

        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, additionalModules, appModuleInTestsError = true)

        echo("Running tests for chain: $blockchain")
        val res = RellApiRunTests.runTests(testConf, sourceDir, appModules, testModules)
        if (testReport) {
            Files.writeString(testReportPath.resolve("${blockchain}-tests.xml"), res.xmlTestReport(blockchain))
        }
        if (sqlLogType != SqlLoggingType.NONE) {
            sqlStatisticsRenderer.display(sqlStatisticsCollector.getStatistics())
        }
        printResults(res)

        codeCoverageCollector.writeReport(testReportPath.resolve("${blockchain}-coverage.xml"))
    }

    private fun createTestConfig(testModuleArgs: Map<String, Map<String, Gtv>>, additionalGtxModules: List<String>? = null,
                                 appModuleInTestsError: Boolean = false): RellApiRunTests.Config {
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(testModuleArgs)
                .cliEnv(CliktCliEnv(this))
                .includeTestSubModules(true)
                .appModuleInTestsError(appModuleInTestsError)
                .addWhitelistedGTXModules(additionalGtxModules)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(settings.model.compile.langVersion)
                .quiet(settings.model.compile.quiet)
                .build()
        return RellApiRunTests.Config.Builder()
                .compileConfig(compileConf)
                .testPatterns(tests)
                .databaseUrl(if (useDB) "${settings.model.databaseUrl}&currentSchema=${settings.model.databaseSchema}_tests" else null)
                .stopOnError(failOnError ?: settings.model.test.failOnError)
                .coverage(coverage == true)
                .sqlErrorLog(settings.model.logSqlErrors)
                .logPrinter { s -> echo("${if (timestamp) LocalTime.now().format(timeFormatter) else ""}$s") }
                .outPrinter(::echo)
                .printTestCases(false)
                .onTestCaseStart { case ->
                    case.print()
                    sqlStatisticsCollector.onTestCaseStart(case)
                }
                .onTestCaseFinished { res ->
                    res.print()
                    if (coverage == true) codeCoverageCollector.onTestCaseFinished(res)
                    sqlStatisticsCollector.onTestCaseFinished(res)
                }
                .sqlLog(false)
                .apply {
                    if (sqlLogType != SqlLoggingType.NONE) {
                        onSqlExecutionFinished(sqlStatisticsCollector::onSqlExecutionFinished)
                    }
                }
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
        echo("${if (timestamp) LocalTime.now().format(timeFormatter) else ""}${info("TEST")}: $name")
    }

    private fun UnitTestCaseResult.print() {
        if (res.isOk) {
            echo("${if (timestamp) LocalTime.now().format(timeFormatter) else ""}${success(res.toString())}: $case (${UnitTestResult.durationToString(res.duration)})")
        } else {
            echo("${if (timestamp) LocalTime.now().format(timeFormatter) else ""}${warning(res.toString())}: $case (${UnitTestResult.durationToString(res.duration)})")
        }
    }

    private fun printResults(results: UnitTestRunnerResults) {
        val failedTests = results.getResults().filter { it.res.error != null }

        results.print { msg ->
            val decorated = decorateMessage(msg, mapOf(
                    "OK" to success,
                    "***** OK *****" to success,
                    "Error:" to danger,
                    "FAILED TESTS:" to info,
                    "FAILED" to danger,
                    "***** FAILED *****" to danger,
                    "Actual:" to success,
                    "Expected:" to danger,
                    "Diff:" to warning,
                    "Stack trace:" to info
            ))
            echo(decorated)
        }

        if (failedTests.isEmpty()) {
            currentContext.terminal.success("")
        } else {
            throw CliktError("")
        }
    }

    private fun decorateMessage(msg: String, colors: Map<String, TextStyle>): String {
        return msg.lines().joinToString(separator = System.lineSeparator()) {
            decorateLineStart(it, colors)
        }
    }

    private fun decorateLineStart(line: String, colors: Map<String, TextStyle>): String {
        for ((prefix, style) in colors) {
            if (line.startsWith(prefix)) {
                return line.replaceFirst(prefix, style(prefix))
            }
        }
        return line
    }
}

