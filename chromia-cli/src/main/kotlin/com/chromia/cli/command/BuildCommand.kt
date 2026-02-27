package com.chromia.cli.command

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.cli.tools.config.ConfigurationFormat
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.config.configurationFormatOption
import com.chromia.cli.util.BuildCliEnv
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.hideLibWarningsOption
import com.chromia.cli.util.skipLibCheckOption
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple

class BuildCommand : ChromiaCommand(help = "Build an application and create a blockchain configuration") {
    override val invokeWithoutSubcommand: Boolean
        get() = true

    private val blockchain by blockchainOption(
        "Explicitly specify which blockchain(s) to compile",
        "BLOCKCHAIN"
    ).multiple()
    // TODO: add a flag to skip libraries verification
    private val settings by chromiaModelOption()
    private val format by configurationFormatOption()
    private val hideLibWarnings by hideLibWarningsOption()
    private val skipLibCheck by skipLibCheckOption()

    override fun run() {
        val chromiaModel = settings.model.filterBlockchains(blockchain)
        ChromiaCompileApi.build(
                BuildCliEnv(this, hideLibWarnings),
                chromiaModel,
                !skipLibCheck
        )
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
