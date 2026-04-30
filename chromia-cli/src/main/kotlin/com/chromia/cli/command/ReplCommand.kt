package com.chromia.cli.command

import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelOption
import com.chromia.cli.tools.config.safeOptionalChromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.util.OutputFormat
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.logSqlOption
import com.chromia.cli.util.module
import com.chromia.cli.util.outputFormat
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.deprecated
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.inputStream
import com.google.common.base.Throwables
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvString
import net.postchain.rell.api.base.RellApiCompile
import net.postchain.rell.api.shell.RellApiRunShell
import net.postchain.rell.base.compiler.base.utils.C_Message
import net.postchain.rell.base.compiler.base.utils.C_Parser
import net.postchain.rell.base.model.ModuleName
import net.postchain.rell.base.repl.ReplInputChannel
import net.postchain.rell.base.repl.ReplInputChannelFactory
import net.postchain.rell.base.repl.ReplOutputChannel
import net.postchain.rell.base.repl.ReplOutputChannelFactory
import net.postchain.rell.base.repl.ReplValueFormat
import net.postchain.rell.base.repl.ReplValueFormatter
import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.Rt_Value
import net.postchain.rell.base.runtime.utils.Rt_Utils
import java.io.BufferedReader
import java.io.File
import kotlin.time.measureTime


class ReplCommand : ChromiaCommand(help = """
    REPL is used to create a language shell for Rell that takes single user inputs, executes them, and returns the result.
    Inside the repl you can create local variables and execute Rell commands, it can be attached to a Rell module to be able
    to inspect a dapp state and execute dapp functionalities.
    
    Run query commands:
    To be able to run queries in the shell, a user must have a module defined from the start of the repl command in 
    which the query is defined. Queries that do not depend on entities can be executed without a database connection, and queries
    that depend on an entity must then have a database connection defined.
    
    Run Operations commands:
    When a operation is executed from the repl shell, the database connection and a module needs to be defined from the start of the repl command,
    to be able to execute  an operation it needs added to a transaction. 
    This can be done by wrapping it with a test transaction like this: `rell.test.tx(<your operation>..).run()`
    
    Rell scripts:
    You can specify a file with Rell statements which will be read and executed (specifying `-` will read from standard input).
    Command line arguments can be specified and will be available as `args: list<text>`.
    This can not be combined with the `-c` option.
    Support for Rell scripts is experimental and may be changed or removed at any time.
""".trimIndent()) {
    private val settings by safeOptionalChromiaModelOption()
    private val sqlLog by logSqlOption()
    private val historyFile by option(help = "Save command history to this file").file(canBeDir = false, mustBeWritable = true)
    private val useDB by option(help = "If a session towards the configured database should be established").flag()
    private val command by option("-c", "--command", help = "Execute a single command", metavar = "COMMAND")
            .validate { require(it.isNotBlank()) }
    private val rawOutput by option("-r", "--raw-output", help = "Will print large object line by line and strings without quotes").flag()
            .deprecated("Use `--output-format raw` instead")
    private val outputFormat by outputFormat()
    private val printDuration by option("-d", "--duration", help = "Print duration of the execution").flag()
    private val moduleOrBlockchain by mutuallyExclusiveOptions(
            name = "Module source",
            option1 = module().convert { ModuleSource.FromModule(it) },
            option2 = blockchainOption(
                help = "Name of the blockchain from which to load the module and moduleArgs"
            ).convert { ModuleSource.FromBlockchain(it) }
    ).single()

    private val script by argument(name = "script", help = "Script file")
            .inputStream()
            .optional()

    private val args by argument(name = "args", help = "Arguments to script").multiple()

    private val logPrefix = "[:<console>(<console>:1)] "

    override fun run() {
        val duration = measureTime {
            if (script != null && command != null) {
                throw UsageError("Cannot use -c when specifying script file")
            }

            if (useDB && settings.model == null) {
                throw CliktError("To correctly connect to the database, specifying the settings file is required")
            }
            val localModel = settings.model ?: ChromiaModel.default()
            val sourceDir = settings.sourceDir ?: localModel.compile.source.toFile()
            val testModuleArgs= settings.model?.test?.moduleArgs ?: mapOf()

            val (selectedModule, selectedBlockchain) = when (val selected = moduleOrBlockchain) {
                is ModuleSource.FromModule ->  selected.module.str() to null
                is ModuleSource.FromBlockchain -> {
                    val module = localModel.blockchains[selected.blockchain]?.module
                    module to selected.blockchain
                }
                else -> null to null
            }

            val moduleArgs = selectedBlockchain?.let {
                localModel.blockchains[it]?.moduleArgs
            } ?: mapOf()

            val compileConfig = RellApiCompile.Config.Builder()
                    .moduleArgs(mergeModuleArgs(moduleArgs, testModuleArgs))
                    .cliEnv(CliktCliEnv(this))
                    .mountConflictError(false)
                    .quiet(localModel.compile.quiet)
                    .build()

            val shellConfigBuilder = RellApiRunShell.Config.Builder()
                    .compileConfig(compileConfig)
                    .databaseUrl(if (useDB) "${localModel.databaseUrl}&currentSchema=${localModel.databaseSchema}" else null)
                    .historyFile(historyFile)
                    .outPrinter(::echo)
                    .logPrinter {
                        val msg = if (it.startsWith(logPrefix)) it.substring(logPrefix.length) else it
                        echo(msg, err = true)
                    }
                    .outputChannelFactory(CliktOutputChannelFactory(
                            !terminal.terminalInfo.outputInteractive || command != null || script != null
                    ))
                    .sqlErrorLog(localModel.logSqlErrors)
                    .sqlLog(sqlLog)
                    .printIntroMessage(terminal.terminalInfo.outputInteractive && command == null && script == null)

            if (script != null) {
                script!!.bufferedReader().use { reader ->
                    val shellConfig = shellConfigBuilder.apply {
                        inputChannelFactory(ScriptCommandInputChannelFactory(reader))
                    }
                            .build()
                    RellApiRunShell.runShell(shellConfig, sourceDir, selectedModule)
                }
            } else {
                val shellConfig = shellConfigBuilder.apply {
                    if (command != null)
                        inputChannelFactory(IteratorCommandInputChannelFactory(listOf(command!!)))
                }
                        .build()
                RellApiRunShell.runShell(shellConfig, sourceDir, selectedModule)
            }
        }

        if (printDuration) {
            echo("Script took $duration to run.", err = true)
        }
    }

    private inner class ScriptCommandInputChannelFactory(rawReader: BufferedReader) : ReplInputChannelFactory {
        private val reader = ScriptReader(rawReader)
        override fun createInputChannel(historyFile: File?) = object : ReplInputChannel {
            override fun readLine(prompt: String): String? {
                val line = reader.readLine()
                if (line == null) return null
                val statement = StringBuilder(line)
                while (shouldReadMore(statement.toString())) {
                    val nextLine = reader.readLine()
                    if (nextLine != null) {
                        statement.append(nextLine)
                        statement.append('\n')
                    }
                }
                return statement.toString()
            }

            // TODO using internal Rell API, replace with public API when available
            private fun shouldReadMore(line: String): Boolean =
                    line.isNotBlank() && C_Parser.checkEofErrorRepl(line) != null
        }
    }

    private inner class ScriptReader(val reader: BufferedReader) {
        private var first = true
        private var line: String? = null

        fun readLine(): String? {
            if (first) {
                first = false
                line = reader.readLine()
                if (line != null && line!!.startsWith("#!")) {
                    line = ""
                }
                return "val args: list<text> = [${args.joinToString(", ") { GtvString(it).toString() }}];"
            } else {
                val lastLine = line
                line = reader.readLine()
                return lastLine
            }
        }
    }

    private class IteratorCommandInputChannelFactory(val commands: Iterable<String>) : ReplInputChannelFactory {
        override fun createInputChannel(historyFile: File?) = object : ReplInputChannel {
            val commandIterator = commands.iterator()
            override fun readLine(prompt: String) = if (commandIterator.hasNext()) commandIterator.next() else null
        }
    }

    private inner class CliktOutputChannelFactory(private val failOnError: Boolean) : ReplOutputChannelFactory {
        private var valueFormat = if (rawOutput)
            ReplValueFormat.ONE_ITEM_PER_LINE
        else
            when (outputFormat) {
                OutputFormat.pretty -> ReplValueFormat.GTV_STRING
                OutputFormat.raw -> ReplValueFormat.ONE_ITEM_PER_LINE
                OutputFormat.JSON -> ReplValueFormat.GTV_JSON
                OutputFormat.XML -> ReplValueFormat.GTV_XML
                else -> throw UsageError("Unsupported output format $outputFormat", paramName = "--output-format")
            }

        override fun createOutputChannel() = object : ReplOutputChannel {
            override fun printInfo(msg: String) = echo(msg)
            override fun printCompilerError(code: String, msg: String) = if (failOnError) throw PrintMessage(msg, 1) else echo(msg, err = true)
            override fun printCompilerMessage(message: C_Message) = if (failOnError) throw PrintMessage(message.toString(), 1) else echo(message)
            override fun printControl(code: String, msg: String) = echo(msg)
            override fun printPlatformRuntimeError(e: Throwable) {
                val message = "Run-time error: " + Throwables.getStackTraceAsString(e).trim()
                if (failOnError) throw PrintMessage(message, 1)
                echo(message)
            }

            override fun printRuntimeError(e: Rt_Exception) {
                val baseMessage = "Run-time error: ${e.message}"
                val message = Rt_Utils.appendStackTrace(baseMessage, e.info.stack)
                if (failOnError) throw PrintMessage(message, 1)
                echo(message)
            }

            override fun printValue(value: Rt_Value) {
                ReplValueFormatter.format(value, valueFormat)?.let { echo(it) }
            }

            override fun setValueFormat(format: ReplValueFormat) {
                valueFormat = format
            }
        }
    }

    fun mergeModuleArgs(
        first: Map<String, Map<String, Gtv>>,
        second: Map<String, Map<String, Gtv>>
    ) = second.entries.fold(first.toMutableMap()) { acc, (key, value) ->
            acc.merge(key, value) { old, new -> old + new }; acc
        }

    sealed class ModuleSource {
        data class FromModule(val module: ModuleName) : ModuleSource()
        data class FromBlockchain(val blockchain: String) : ModuleSource()
    }
}
