package com.chromia.cli.command.generate

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.optionalChromiaModelOption
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.targetDirectoryOption
import com.chromia.rell.dokka.RellDokkaGenerator
import com.chromia.rell.dokka.config.RellDokkaPluginConfigurationBuilder
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.net.URI

class GenerateDocsSiteCommand : ChromiaCommand(
        name = "docs-site",
        help = """
            Generate a documentation site for a dapp ontology
        """.trimIndent()
) {
    private val settings by optionalChromiaModelOption()
    private val model by lazy { settings.model!! }

    private val system by option(hidden = true).flag()
    private val systemIncludes by option("-si", "--system-include", hidden = true)
            .file(mustExist = true, canBeDir = false, mustBeReadable = true)
            .split(",")
            .default(listOf())
            .validate { files -> require(files.all { it.extension == "md" }) { "File(s) must be in markdown format ${files.joinToString { it.path }}" } }

    private val target by targetDirectoryOption(help = "Directory to generate code in")

    private val include by option("-i", "--include", help = """
        Libs to actively include in the navigation of the generated docs site, by default all are excluded.
        To include a library, the full identifier must be specified as an example for the library foo, the inclusion of it
        would be lib.foo
    """.trimIndent()
    ).multiple()

    private val hideLibWarnings by hideLibWarningsOption()

    fun getFilteredModules(libs: Set<String>, explicitIncluded: List<String>): List<String> {
        return libs.minus(explicitIncluded.toSet()).toList()
    }

    private fun formatLibraryPaths() = model.libs.keys.map { "lib.$it" }.toSet()

    val additionalModules by lazy { model.docs.additionalModules ?: emptyList() }

    val mainModules by lazy {
        settings.model!!.blockchains.map {
            it.value.module ?: throw CliktError("Cannot generate docs for ${it.key} without main module")
        }
    }

    override fun run() {

        require(system || settings.model != null) { "Project settings file not found" }
        require(!system || target != null) { "Please specify target folder when generating system docs" }
        val targetFolder = target ?: File(settings.targetDir!!, "site")
        val buildCliEnv = BuildCliEnv(this, hideLibWarnings)

        val builder = configBuilder()
                .targetFolder(targetFolder)
                .cliEnv(buildCliEnv)
        RellDokkaGenerator(builder)
                .generate()
        echo("Documentation generated at $targetFolder")
    }

    private fun configBuilder() = when (system) {
        true -> RellDokkaPluginConfigurationBuilder.SYSTEM.includes(systemIncludes)
        else -> RellDokkaPluginConfigurationBuilder(
                title = model.docs.title,
                modules = mainModules,
                projectRoot = settings.sourceDir!!
        )
                .customStyleSheets(model.docs.customStyleSheets)
                .customAssets(model.docs.customAssets)
                .includes(model.docs.additionalContentFiles)
                .footerMessage(model.docs.footerMessage)
                .filteredModules(getFilteredModules(formatLibraryPaths(), include))
                .additionalModules(additionalModules)
                .apply {
                    model.docs.sourceLink?.let {
                        val localDirectory = model.compile.source
                        addSourceLink(localDirectory.toString(), URI(it.remoteUrl).toURL(), it.remoteLineSuffix)
                    }
                }
    }
}
