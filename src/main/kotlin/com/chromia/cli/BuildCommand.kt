package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.BlockchainConfigurationWriter.storeConfig
import com.chromia.cli.compile.config.NamedBlockchainRid
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.showBridOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import net.postchain.gtv.Gtv
import net.postchain.rell.compiler.base.utils.C_SourceDir
import net.postchain.rell.utils.RellCliEnv
import java.io.File


class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val showBrid by showBridOption()
    private val settings by settingsOption()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        compile(CliktCliEnv(this), settings.source, settings.target, settings.compile, settings.blockchains).apply {
            if (showBrid) this.forEach { (t, u) -> echo("${t.name} ${t.blockchainRid}") }
        }
    }

    companion object {
        fun compile(cliEnv: RellCliEnv, source: File, target: File, compileModel: CompileModel, blockchains: Map<String, BlockchainModel>): Map<NamedBlockchainRid, Gtv> {
            return BlockchainConfigurationGenerator(cliEnv, compileModel, blockchains, C_SourceDir.diskDir(source))
                    .generate()
                    .onEach { (namedBlockchainRid, gtv) -> storeConfig(gtv, namedBlockchainRid.name, target.toPath()) }
        }
    }
}
