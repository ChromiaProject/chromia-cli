package com.chromia.cli.command

import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.launcher.createAliases
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.path
import net.postchain.rell.toolbox.formatter.FormatterOptions
import net.postchain.rell.toolbox.formatter.RellFormatter
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

class FormatCommand : CliktCommand(help = "Automatically format Rell code. Configurable using .rell_format file", invokeWithoutSubcommand = true) {
    override fun aliases() = createAliases()

    private val settings by chromiaModelOption()
    private val sourceDir by option(help = "source directory").path(
            mustExist = true,
            mustBeReadable = true,
            mustBeWritable = true,
            canBeDir = true,
            canBeFile = false
    )
    private val file by option(help = "single Rell file").path(
            mustExist = true,
            mustBeReadable = true,
            mustBeWritable = true,
            canBeDir = false,
            canBeFile = true
    )
    private val formatterOptionsFile by option("--formatter-options", "-fo", help = "Formatter options file (default '${FormatterOptions.PREFERRED_RELL_FORMAT_FILE_NAME}')").file(
            mustExist = true,
            mustBeReadable = true,
            canBeDir = false,
            canBeFile = true
    )

    override fun run() {
        val formatterOptions = FormatterOptions()
        val theFormatterOptionsFile = formatterOptionsFile
                ?: File(settings.projectFolder, FormatterOptions.PREFERRED_RELL_FORMAT_FILE_NAME)
        if (theFormatterOptionsFile.isFile) {
            formatterOptions.updateOptionsFromFile(theFormatterOptionsFile)
        }

        if (file != null) {
            formatRellFile(file!!.toString(), file!!, formatterOptions)
        } else {
            val theSourceDir = sourceDir ?: settings.sourceDir.toPath()
            Files.find(theSourceDir, Int.MAX_VALUE, { path, attributes ->
                attributes.isRegularFile && path.toString().endsWith(".rell")
            }).use {
                it.forEach { rellFile ->
                    formatRellFile(theSourceDir.relativize(rellFile).toString(), rellFile, formatterOptions)
                }
            }
        }
    }

    private fun formatRellFile(filePathToPrint: String, rellFile: Path, formatterOptions: FormatterOptions) {
        echo("Formatting $filePathToPrint... ", trailingNewline = false)
        val sourceText = rellFile.readText()
        val formattedText = RellFormatter.formatString(sourceText, formatterOptions)
        if (formattedText != sourceText) {
            rellFile.writeText(formattedText)
            echo("changed")
        } else {
            echo("no changes")
        }
    }
}
