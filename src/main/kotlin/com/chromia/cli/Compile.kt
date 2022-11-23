package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
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
    private val config by configFile()
    private val nodeConfig by nodeConfigFile()
    private val target by deployTarget()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {

        val nodeAppConf = nodeConfig?.let { NodeConfig.getNodeConfig(it) } ?: NodeConfig.getDefaultNodeConfig(config)
        val cSourceDir = C_SourceDir.diskDir(sourceFile)

        BlockchainConfigurationGenerator(config, cSourceDir, nodeAppConf).generateMaps().toList().forEach {(brid, gtv) ->

            if (target != null) {
                val selectiveDeployment = config.deployment[target]
                generateConfig(gtv, selectiveDeployment, "${target}_${brid}", brid, outputDir).apply {
                    if (showBrid) echo(specifiedBlockchainRid ?: generatedBlockchainRid)
                }

            } else {
                config.deployment.toList().forEach{ (name, model) ->
                    generateConfig(gtv, model, "${name}_${brid}", brid, outputDir).apply {
                        if (showBrid) echo(specifiedBlockchainRid ?: generatedBlockchainRid)
                    }
                }
            }
        }
    }


}