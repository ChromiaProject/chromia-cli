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
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.rell.toolbox.seeder.SeederApi
import net.postchain.rell.toolbox.seeder.SeederParams


class SeederGenerateCommand: ChromiaCommand(name = "generate", help = "Generate Rell blockchain seeder module") {
    private val settings by chromiaModelOption()
    private val alternativeConfigFolder by option(help = "Alternative path to the root seeder configuration folder")
            .file(mustExist = true, canBeDir = true, canBeFile = false, mustBeReadable = true)
    private val blockchains by blockchainOption(help = "Blockchains to generate seeders for (defaults to all)", metavar = "BLOCKCHAIN")
            .multiple()


    override fun run() {
        val sourceDir = settings.model.compile.source
        val rellVersion = settings.model.compile.rellVersion
        val rootConfigFolder = alternativeConfigFolder ?: settings.projectFolder.resolve(DEFAULT_SEEDER_CONFIG_FOLDER)

        val blockchainsModels = filterOutBlockchains(blockchains, settings)
        blockchainsModels.forEach { blockchain ->
            val blockchainName = blockchain.key

            val seedOutputPath = sourceDir.resolve("seeder/seed_$blockchainName.rell")
            val blockchainModule = blockchain.value.module ?: throw PrintMessage("Cannot generate seed for blockchain $blockchainName without main module")

            // TODO: Validate for Windows
            val seedConfig = rootConfigFolder.resolve("$blockchainName/seeder.yml")
            val isLibrary = blockchain.value.type == BlockchainModel.Type.LIBRARY

            if (!seedConfig.exists()) { echo("No seeder configuration found for '$blockchainName', skipping generation"); return@forEach}

            val seederParams = SeederParams.Builder()
                    .sourceDir(sourceDir.toFile())
                    .outputPath(seedOutputPath)
                    .modules(listOf(blockchainModule))
                    .rellVersion(rellVersion)
                    .rootConfig(seedConfig.toPath())
                    .isLibrary(isLibrary)
                    .mountName(blockchainName)
                    .build()

            try {
                SeederApi.generate(seederParams)
                echo("Seeder rell module generated at ${seederParams.outputPath}")
            } catch (e: Exception) {
                throw PrintMessage("Failed to generate '$blockchainModule' seeder module for '$blockchainName': ${e.message}", statusCode = 1)
            }
        }
    }
}
