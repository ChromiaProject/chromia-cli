package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.DeploymentConfigGenerator.generateConfig
import com.chromia.cli.compile.config.NamedBlockchainRid
import com.chromia.cli.model.ChromiaCliModel
import com.chromia.cli.util.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import net.postchain.gtv.Gtv
import net.postchain.rell.compiler.base.utils.C_SourceDir


class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration") {
    private val showBrid by showBridOption()
    private val settings by settingsOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        compile(settings).apply {
            if (showBrid) this.forEach { (t, u) -> echo("${t.name} ${t.blockchainRid}") }
        }
    }

    companion object {
        fun compile(settings: ChromiaCliModel): Map<NamedBlockchainRid, Gtv> {
            return BlockchainConfigurationGenerator(settings, C_SourceDir.diskDir(settings.compile.source), null)
                    .generate()
                    .onEach { (namedBlockchainRid, gtv) ->
                        generateConfig(gtv, null, namedBlockchainRid.name, namedBlockchainRid.blockchainRid, settings.compile.target.toPath(), namedBlockchainRid.name)
                    }
        }
    }
}
