package com.chromia.cli.command.generate

import com.chromia.api.ChromiaGenerateApi
import com.chromia.cli.tools.config.optionalChromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.rell.dokka.RellDokkaGenerator
import com.chromia.rell.dokka.config.RellDokkaPluginConfigurationBuilder
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file

class GenerateDocsSiteCommand : CliktCommand(
        name = "docs-site",
        help = """
            Generate a documentation site for a dapp ontology
        """.trimIndent()
) {
    private val settings by optionalChromiaModelOption()

    private val system by option(hidden = true).flag()
    private val systemIncludes by option("-si", "--system-include", hidden = true)
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)
            .split(",")
            .default(listOf())
            .validate { files -> require(files.all { it.extension == "md" }) { "File(s) must be in markdown format ${files.joinToString { it.path }}" } }

    private val target by option("-d", "--target", help = "Directory to generate code in")
            .file(canBeFile = false)

    override fun run() {
        require(system || settings.model != null) { "Project settings file not found" }
        require(!system || target != null) { "Please specify target folder when generating system docs" }
        if (system) return generateSystemDocs()
        ChromiaGenerateApi.docsSite(CliktCliEnv(this), settings.model!!, settings.projectFolder!!.toPath(), target?.toPath())
    }

    private fun generateSystemDocs() {
        RellDokkaPluginConfigurationBuilder.SYSTEM
                .includes(systemIncludes)
                .targetFolder(target!!)
                .apply { RellDokkaGenerator(this).generate() }
        echo("Documentation generated at $target")
    }
}
