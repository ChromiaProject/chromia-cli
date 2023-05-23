package com.chromia.cli

import com.chromia.cli.util.AnsiColorScheme
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.ColorAware
import com.chromia.cli.util.ColorFormat
import com.chromia.cli.util.NoColorScheme
import com.chromia.cli.util.green
import com.chromia.cli.util.line
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
import net.postchain.rell.runtime.Rt_Exception
import net.postchain.rell.runtime.Rt_Printer
import net.postchain.rell.runtime.utils.Rt_Utils
import net.postchain.rell.utils.TestCase
import net.postchain.rell.utils.TestCaseResult
import net.postchain.rell.utils.TestRunnerResults
import net.postchain.rell.utils.cli.RellCliApi
import net.postchain.rell.utils.cli.RellCliCompileConfig
import net.postchain.rell.utils.cli.RellCliException
import net.postchain.rell.utils.cli.RellCliRunTestsConfig


class TestCommand : CliktCommand(help = "Run tests in working directory"), ColorAware {

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
        val testModules = modules ?: settings.test.modules

        val printer = object : Rt_Printer {
            override fun print(str: String) = echo(str)
        }
        val compileConf = RellCliCompileConfig.Builder()
                .moduleArgs(settings.test.moduleArgs)
                .cliEnv(CliktCliEnv(this))
                .includeTestSubModules(true)
                .appModuleInTestsError(false)
                .moduleArgsMissingError(true)
                .mountConflictError(true)
                .version(settings.compile.langVersion)
                .quiet(settings.compile.quiet)
                .build()
        val testConf = RellCliRunTestsConfig.Builder()
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

        try {
            val res = RellCliApi.runTests(testConf, sourceDir, listOf(), testModules)
            printResults(res)
        } catch (e: RellCliException) {
            throw CliktError(e.message)
        }
    }


    private fun TestCase.print() {
        echo("${colorScheme.blue.format("TEST")}: $name")
    }

    private fun TestCaseResult.print() {
        if (res.isOk) {
            green("$res $case")
        } else {
            red("$res $case")
        }
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
            throw CliktError(colorScheme.red.format("***** FAILED *****"))
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
}
