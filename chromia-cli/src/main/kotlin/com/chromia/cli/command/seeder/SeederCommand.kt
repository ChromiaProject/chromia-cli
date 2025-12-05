package com.chromia.cli.command.seeder

import com.chromia.cli.command.NoOpChromiaCommand
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.tools.config.ChromiaModelOption
import com.github.ajalt.clikt.core.subcommands

class SeederCommand: NoOpChromiaCommand(help = "Generate fake data for a local database") {

    companion object {
        fun commands() = SeederCommand().subcommands(
                SeederInitConfigCommand(),
                SeederGenerateCommand()
        )

        const val DEFAULT_SEEDER_CONFIG_FOLDER = ".chromia/seeder"
    }
}

fun filterOutBlockchains(blockchains: List<String>, settings: ChromiaModelOption): Map<String, BlockchainModel> {
    return if (blockchains.isEmpty()) {
        settings.model.blockchains
    }
    else {
        settings.model.blockchains.filter { it.key in blockchains }
    }
}