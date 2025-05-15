package com.chromia.cli.command.seeder

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.seeder.SeederCommand.Companion.DEFAULT_SEEDER_CONFIG_FOLDER
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.targetDirectoryOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.multiple
import net.postchain.rell.toolbox.seeder.InitialConfigurationParams
import net.postchain.rell.toolbox.seeder.SeederApi

class SeederInitConfigCommand: ChromiaCommand(name = "init", help = "Create initial seeder configuration for blockchains") {

    private val settings by chromiaModelOption()
    private val blockchains by blockchainOption(help = "Blockchains to generate configuration for (defaults to all).", metavar = "BLOCKCHAIN")
            .multiple()


    override fun run() {
        val sourceDir = settings.model.compile.source
        val projectFolder = settings.projectFolder
        val outputFolder = projectFolder.resolve(DEFAULT_SEEDER_CONFIG_FOLDER)
        val rellVersion = settings.model.compile.rellVersion
        val blockchainsModels = filterOutBlockchains(blockchains, settings)

        blockchainsModels.forEach { blockchain ->
            val blockchainOutputFolder = outputFolder.resolve(blockchain.key)
            val blockchainModule = blockchain.value.module ?: throw PrintMessage("Cannot generate seeder configuration for blockchain ${blockchain.key} without main module")
            val isLibrary = blockchain.value.type == BlockchainModel.Type.LIBRARY

            val config = InitialConfigurationParams.Builder()
                    .sourceDir(sourceDir.toFile())
                    .outputPath(blockchainOutputFolder.toPath())
                    .modules(listOf(blockchainModule))
                    .rellVersion(rellVersion)
                    .isLibrary(isLibrary)
                    .build()

            try {
                SeederApi.generateDefaultConfigurations(config)
                echo("Seeder configuration generated at ${config.outputPath}")
            } catch (e: Exception) {
                throw PrintMessage("Failed to generate seeder config file: ${e.message}", statusCode = 1)
            }

        }

    }
}
