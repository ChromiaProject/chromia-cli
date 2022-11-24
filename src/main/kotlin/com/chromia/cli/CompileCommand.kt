package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator.generateConfig
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import net.postchain.rell.compiler.base.utils.C_SourceDir


class CompileCommand : CliktCommand(help = "Compile an application and create a blockchain configuration") {
    private val sourceFile by sourceDirOption()
    private val outputDir by outputDirOption()
    private val showBrid by showBridOption()
    private val settings by settingsOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        BlockchainConfigurationGenerator(settings, C_SourceDir.diskDir(sourceFile), null).generate().toList().forEach { (brid, gtv) ->
            generateConfig(gtv, null, brid.name, brid.blockchainRid, outputDir).apply {
                if (showBrid) echo("$blockchainName $generatedBlockchainRid")
            }
        }
    }
}
