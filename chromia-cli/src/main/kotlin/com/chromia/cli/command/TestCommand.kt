package com.chromia.cli.command

import com.chromia.build.tools.test.sql.SqlStatisticsCollector
import com.chromia.build.tools.test.sql.htmlSqlLogReport
import com.chromia.build.tools.test.xmlTestReport
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.formatter.danger
import com.chromia.cli.tools.formatter.info
import com.chromia.cli.tools.formatter.success
import com.chromia.cli.tools.formatter.warning
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.addWhitelistedGTXModules
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.modulesOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValueLazy
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.types.boolean
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
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
import kotlin.io.path.name
import kotlin.io.path.relativeTo

val timeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS ")

class TestCommand : ChromiaCommand(help = "Run tests in working directory") {

    private val blockchains by blockchainOption(help = "Run tests for specified blockchain(s).", metavar = "BLOCKCHAIN")
            .multiple()

    sealed class ModulesOrFile {
        data class Modules(val modules: List<String>) : ModulesOrFile()
        data class File(val file: Path) : ModulesOrFile()
        data object All : ModulesOrFile()
    }

    private val modulesOrFile: ModulesOrFile by mutuallyExclusiveOptions(
            modulesOption("Run tests in this module(s) only. Must be the part of the specified modules or its submodules. Comma delimited, will default to all modules either under each selected blockchain or test")
                    .convert { x: List<String> -> ModulesOrFile.Modules(x) },
            option("--file", help = "Run tests in one file").path(canBeFile = true, canBeDir = true, mustExist = true)
                    .convert { ModulesOrFile.File(it) },
    ).single().default(ModulesOrFile.All)

    private val settings by chromiaModelOption()

    private val tests by option(help = "test method pattern", metavar = "").split(",")
    private val sourceDir by lazy { settings.sourceDir }
    private val useDB by option(help = "If a session towards the configured database should be established")
            .flag("--no-db", default = true)
    private val testReport by option(help = "Generate JUnit XML and SQL Queries test reports")
            .flag()
    private val testReportDir by option(help = "JUnit XML test and SQL Queries reports directory (defaults to \"build/reports\")")
            .file(canBeDir = true, canBeFile = false)

    private val failOnError by option(help = "Sets test execution to stop on error and override any \"failOnError\" settings for tests that are in the scope being executed")
            .boolean()
            .optionalValueLazy { true }

    private val timestamp by option("-ts", "--timestamp", help = "Timestamp on logs").flag()

    private val hideLibWarnings by hideLibWarningsOption()

    private val sqlStatisticsCollector by lazy { SqlStatisticsCollector() }

    override fun run() {
        val modules = when (val mf = modulesOrFile) {
            is ModulesOrFile.Modules -> mf.modules
            is ModulesOrFile.File -> listOf(moduleOfFile(sourceDir.toPath(), mf.file))
            is ModulesOrFile.All -> null
        }

        val testReportPath = testReportDir ?: File(settings.targetDir, "reports")
        if (testReport) {
            testReportPath.mkdirs()
        }
        var hasRunTests = false
        try {
            settings.model.blockchains
                    .filter { blockchains.isEmpty() || blockchains.contains(it.key) }
                    .forEach { bc ->
                        val testModules = calcTestModules(modules, bc.value.test.modules)
                        if (testModules.isNotEmpty()) {
                            runTestsForBlockchain(bc.key, bc.value, testModules, testReportPath.toPath())
                            hasRunTests = true
                        }
                    }
            if (blockchains.isEmpty()) {
                val testModules = calcTestModules(modules, settings.model.test.modules)
                if (testModules.isNotEmpty()) {
                    runUnitTests(testModules, testReportPath.toPath())
                    hasRunTests = true
                }
            }
        } catch (e: RellCliException) {
            throw CliktError(e.message)
        }
        if (!hasRunTests) {
            throw CliktError("No tests to run")
        }
    }

    private fun calcTestModules(modules: List<String>?, declaredTestModules: List<String>): List<String> = if (modules.isNullOrEmpty())
        declaredTestModules
    else
        modules.filter { m -> declaredTestModules.any { m.startsWith(it) } }

    private fun runTestsForBlockchain(blockchain: String, chainConfig: BlockchainModel, testModules: List<String>, testReportPath: Path) {
        val appModules = listOf(chainConfig.module
                ?: throw CliktError("Cannot run tests for blockchain $blockchain without main module"))

        val additionalModules = chainConfig.config["gtx"]?.get("modules")?.asArray()?.map { it.asString() }

        val testModuleArgs = mergeModuleArgs(chainConfig.moduleArgs, chainConfig.test.moduleArgs)
        val testConf = createTestConfig(testModuleArgs, additionalModules, appModuleInTestsError = true)

        echo("Running tests for chain: $blockchain")
        val res = RellApiRunTests.runTests(testConf, sourceDir, appModules, testModules)
        if (testReport) {
            Files.writeString(testReportPath.resolve("${blockchain}-tests.xml"), res.xmlTestReport(blockchain))
            Files.writeString(testReportPath.resolve("${blockchain}-tests-sql.html"), sqlStatisticsCollector.getStatistics().htmlSqlLogReport(blockchain))
        }
        printResults(res)
    }

    private fun runUnitTests(testModules: List<String>, testReportPath: Path) {
        val testModuleArgs = settings.model.test.moduleArgs
        val testConf = createTestConfig(testModuleArgs)

        currentContext.terminal.println("=".repeat(20) + "Running unit tests" + "=".repeat(20))
        val res = RellApiRunTests.runTests(testConf, sourceDir, listOf(), testModules)
        if (testReport) {
            Files.writeString(testReportPath.resolve("rell-unit-tests.xml"), res.xmlTestReport("rell"))
            Files.writeString(testReportPath.resolve("rell-unit-tests-sql.html"), sqlStatisticsCollector.getStatistics().htmlSqlLogReport("Rell Unit Tests"))
        }
        printResults(res)
    }

    private fun createTestConfig(testModuleArgs: Map<String, Map<String, Gtv>>, additionalGtxModules: List<String>? = null,
                                 appModuleInTestsError: Boolean = false): RellApiRunTests.Config {
        val compileConf = RellApiCompile.Config.Builder()
                .moduleArgs(testModuleArgs)
                .cliEnv(BuildCliEnv(this, hideLibWarnings))
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
                .sqlErrorLog(settings.model.logSqlErrors)
                .logPrinter { s -> echo("${if (timestamp) LocalTime.now().format(timeFormatter) else ""}$s") }
                .outPrinter(::echo)
                .printTestCases(false)
                .onTestCaseStart { case ->
                    case.print()
                    if (testReport) sqlStatisticsCollector.onTestCaseStart(case)
                }
                .onTestCaseFinished { res ->
                    res.print()
                    if (testReport) sqlStatisticsCollector.onTestCaseFinished(res)
                }
                .sqlLog(false)
                .apply {
                    if (testReport) {
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

internal fun moduleOfFile(sourceDir: Path, file: Path): String {
    val (resolvedFile, resolvedSourceDir) = if (sourceDir.isAbsolute) {
        file.toAbsolutePath().toRealPath() to sourceDir.toRealPath()
    } else {
        file to sourceDir
    }
    return resolvedFile.relativeTo(resolvedSourceDir).joinToString(separator = ".") { it.name }.let {
        if (it.endsWith(".rell")) it.dropLast(5) else it
    }
}
