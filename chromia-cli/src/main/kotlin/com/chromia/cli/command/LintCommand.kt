package com.chromia.cli.command

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import java.io.File
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.toPath
import kotlin.io.path.writeText
import net.postchain.rell.api.base.RellCliExitException
import net.postchain.rell.toolbox.core.indexer.WorkspaceIndexer
import net.postchain.rell.toolbox.formatter.FormatterOptions
import net.postchain.rell.toolbox.linter.AutoFixer
import net.postchain.rell.toolbox.linter.FormattingStyleLinter
import net.postchain.rell.toolbox.linter.LinterOptions
import net.postchain.rell.toolbox.linter.RellLinter

class LintCommand : CliktCommand(help = "Analyze Rell code to find potential issue and coding style violations. Configurable using .rell_lint file", invokeWithoutSubcommand = true) {
    override fun aliases() = createAliases()

    private val settings by chromiaModelOption()
    private val sourceDir by option(help = "source directory").path(
            mustExist = true,
            mustBeReadable = true,
            mustBeWritable = true,
            canBeDir = true,
            canBeFile = false
    ).convert { it.toAbsolutePath().normalize() }

    private val formatterOptionsFile by option("--formatter-options", "-fo", help = "Formatter options file (default '${FormatterOptions.PREFERRED_RELL_FORMAT_FILE_NAME}')").file(
            mustExist = true,
            mustBeReadable = true,
            canBeDir = false,
            canBeFile = true
    )
    private val linterOptionsFile by option("--linter-options", "-lo", help = "Linter options file (default '${LinterOptions.CONFIG_FILE_NAME}')").file(
            mustExist = true,
            mustBeReadable = true,
            canBeDir = false,
            canBeFile = true
    )
    private val fix by option("--fix", help = "Fix all auto-fixable issues").flag()

    private val autoFixer = AutoFixer()

    override fun run() {
        val formatterOptions = FormatterOptions()
        val theFormatterOptionsFile = formatterOptionsFile
                ?: File(settings.projectFolder, FormatterOptions.PREFERRED_RELL_FORMAT_FILE_NAME)
        if (theFormatterOptionsFile.isFile) {
            formatterOptions.updateOptionsFromFile(theFormatterOptionsFile)
        }

        val linterOptions = LinterOptions(enabled = true)
        val theLinterOptionsFile = linterOptionsFile
                ?: File(settings.projectFolder, LinterOptions.CONFIG_FILE_NAME)
        if (theLinterOptionsFile.isFile) {
            linterOptions.updateOptionsFromFile(theLinterOptionsFile)
        }

        val theSourceDir = sourceDir ?: settings.sourceDir.toPath()

        val indexer = runIndexer(theSourceDir, formatterOptions, linterOptions)
        if (fix) {
            fixAutoFixableIssues(indexer, theSourceDir)
        } else {
            reportIssues(indexer, theSourceDir)
        }
    }

    private fun fixAutoFixableIssues(indexer: WorkspaceIndexer, sourceDir: Path) {
        indexer.getAllLintAndFormatIssues().forEach { (fileUri, issues) ->
            if (issues.isNotEmpty()) {
                val filePath = fileUri.toPath()
                echo("Fixing: ${sourceDir.relativize(filePath)}... ", trailingNewline = false)
                val sourceText = filePath.readText()
                val resource = indexer.getResource(fileUri) ?: return@forEach
                val fixedText = autoFixer.fix(resource, sourceText)
                if (sourceText != fixedText) {
                    filePath.writeText(fixedText)
                    echo("fixed")
                } else {
                    echo("no changes")
                }
            }
        }
    }

    private fun reportIssues(indexer: WorkspaceIndexer, sourceDir: Path) {
        val allIssues = indexer.getAllIssues()
        var issuesFound = false
        allIssues.forEach { (fileUri, fileIssues) ->
            if (fileIssues.isNotEmpty()) {
                echo("${sourceDir.relativize(fileUri.toPath())}")
                issuesFound = true
                fileIssues.forEach { issue ->
                    echo("    ${issue.code} - ${issue.message}")
                }
            }
        }
        if (issuesFound) {
            throw RellCliExitException(1)
        }
    }

    private fun runIndexer(sourceDir: Path, formatterOptions: FormatterOptions, linterOptions: LinterOptions): WorkspaceIndexer {
        val indexer = WorkspaceIndexer(
                sourceDir.toUri(),
                RellLinter(),
                linterOptions,
                FormattingStyleLinter(),
                formatterOptions,
                settings.projectFolder.toURI())
        indexer.initialFileIndexBuild(cachedIndexer = null)
        return indexer
    }
}
