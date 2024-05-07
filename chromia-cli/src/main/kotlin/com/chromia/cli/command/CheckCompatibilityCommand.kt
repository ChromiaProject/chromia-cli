package com.chromia.cli.command

import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.build.tools.config.ChromiaConfigLoader.Companion.DEFAULT_CHROMIA_MODEL_FILENAME
import com.chromia.cli.check_compatilibity.CheckCompatibility
import com.chromia.cli.check_compatilibity.BlockchainSource
import com.chromia.cli.check_compatilibity.LogWrapper
import com.chromia.cli.tools.env.cliEnv
import com.chromia.cli.util.blockchainOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.exception.UserMistake
import java.io.File

class CheckCompatibilityCommand : CliktCommand(
        name = "check-compatibility",
        help = "Simple compatibility check between dapp versions by deploying the first (from) version followed by the second (to) version."
) {

    private val fromFileOption by option(
            "-f", "--from-config", help = "Blockchain dapp to upgrade from. Either a XML config or $DEFAULT_CHROMIA_MODEL_FILENAME file")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()
    private val toFileOption by option(
            "-t", "--to-config", help = "Blockchain dapp to upgrade to. Either a XML config or $DEFAULT_CHROMIA_MODEL_FILENAME file. If not specified the $DEFAULT_CHROMIA_MODEL_FILENAME file in current directory is used")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
    private val blockchainOption by blockchainOption(
            help = "Run check for specified blockchain. Only required if a $DEFAULT_CHROMIA_MODEL_FILENAME file is specified as from/to input")
    private val verbose by option(help = "Show verbose information").flag()

    override fun run() {

        val fromFile = getBlockchainFile(fromFileOption)
        val toFile = getBlockchainFile(toFileOption)

        val blockchain = if (isYmlFile(fromFile) || isYmlFile(toFile)) blockchainOption ?:
            throw PrintMessage("Blockchain (-bc, --blockchain) is required when model file is used as input since it can contain multiple blockchains", statusCode = 1)
        else
            "test-chain"

        val fromBlockchain = loadBlockchain(fromFile, blockchain)
        val toBlockchain = loadBlockchain(toFile, blockchain)

        val checkCompatibility = CheckCompatibility(toBlockchain, logger = LogWrapper(verbose, ::echo))

        echo("Checking compatibility")
        echo("  From application: $fromFile")
        echo("  To application  : $toFile")
        echo()

        try {
            checkCompatibility.upgradeFrom(fromBlockchain, blockchain)

            echo()
            echo("Passed")
        } catch (e: UserMistake) {
            if (e.message != null) {
                echo()
                throw PrintMessage(e.message!!, statusCode = 1)
            }
            throw e
        }
    }

    private fun getBlockchainFile(file: File?): File {
        val absoluteFile = ChromiaConfigLoader(cliEnv()).findModelFile(file) ?:
        throw PrintMessage("Project settings file not found and no to model/config file provided", statusCode = 1)
        return absoluteFile
    }

    private fun loadBlockchain(file: File, blockchain: String): BlockchainSource {

        if (isYmlFile(file)) {
            return BlockchainSource.fromChromiaFile(file)
        } else if (isFileSuffix(file, ".xml")) {
            return BlockchainSource.fromXmlFile(blockchain, file)
        }

        throw PrintMessage("Unsupported file type: $file", statusCode = 1)
    }

    private fun isYmlFile(file: File) =
            isFileSuffix(file, ".yml")

    private fun isFileSuffix(file: File, suffix: String) =
            file.name.lowercase().endsWith(suffix)
}
