package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.createAliases
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.GtvDecoder
import net.postchain.rell.utils.PostchainUtils

class NodeCommand : NoOpCliktCommand(help = "Interract with a test node") {
    override fun aliases() = createAliases()
}


fun nodeCommands() = NodeCommand().subcommands(
        StartCommand(),
        UpdateCommand()
)

abstract class AbstractNodeCommand(help: String) : CliktCommand(help = help) {
    protected val settings by settingsOption()
    protected val blockchainConfigs by option(
            "-bc", "--blockchain-config",
            help = "Manually specify which blockchain-configs to run"
    )
            .file(mustExist = true, canBeDir = false)
            .multiple()

    protected val name by option(help = "Only start specified blockchains (multiple)", metavar = "NAME")
            .multiple()

    private val overrides by option("-p", help = "Override any property value (usage: -p key=value)", metavar = "KEY=VALUE").associate()
    protected val nodeConfig by nodePropertiesOption().defaultLazy { NodeConfig.getDefaultNodeConfig(settings.model, overrides) }

    protected fun extractConfigs(): Collection<BlockchainConfigHolder> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            BuildCommand.compile(CliktCliEnv(this), settings.source, settings.target, settings.compile, settings.blockchains)
        } else {
            blockchainConfigs
                    .associate {
                        if (it.extension == "gtv") {
                            it.nameWithoutExtension to GtvDecoder.decodeGtv(it.inputStream())
                        } else {
                            it.nameWithoutExtension to PostchainUtils.xmlToGtv(it.readText())
                        }
                    }
                    .map { BlockchainConfigHolder.from(it.key, it.value) }

        }.sortedBy { it.name }

        return if (name.isEmpty()) configsToAdd else configsToAdd.filter { name.contains(it.name) }
    }
}
