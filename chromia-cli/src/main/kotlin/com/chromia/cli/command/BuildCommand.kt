package com.chromia.cli.command

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.cli.tools.config.ConfigurationFormat
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.config.configurationFormatOption
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.blockchainOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple

class BuildCommand : ChromiaCommand(help = "Build an application and create a blockchain configuration") {
    override val invokeWithoutSubcommand: Boolean
        get() = true

    private val blockchain by blockchainOption("Explicitly specify which blockchain(s) to compile", "BLOCKCHAIN").multiple()
    private val settings by chromiaModelOption()
    private val format by configurationFormatOption()

    override fun aliases() = createAliases()

    override fun run() {
        ChromiaCompileApi.build(cliEnv(), settings.model.filterBlockchains(blockchain))
                .forEach {
                    when (format) {
                        ConfigurationFormat.GTV -> it.saveAsBinaryGtv(settings.targetDir.toPath())

                        ConfigurationFormat.XML -> try {
                            it.save(settings.targetDir.toPath())
                        } catch (e: IllegalArgumentException) {
                            throw CliktError("Blockchain ${it.name} contains ${e.message} Use --format=GTV")
                        }
                    }
                }
    }
}
