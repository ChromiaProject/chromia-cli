package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.compile.config.NamedBlockchainRid
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvFactory
import net.postchain.rell.utils.PostchainUtils

class NodeCommand : NoOpCliktCommand(help = "Interract with a test node")


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

    private val overrides by option("-p", help = "Override any property value").associate()
    protected val nodeConfig by nodePropertiesOption().defaultLazy { NodeConfig.getDefaultNodeConfig(settings.model, overrides) }

    protected fun extractConfigs(): Map<NamedBlockchainRid, Gtv> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            BuildCommand.compile(settings)
        } else {
            blockchainConfigs
                .associate {
                    if (it.extension == "gtv") {
                        it.nameWithoutExtension to GtvDecoder.decodeGtv(it.inputStream())
                    } else {
                        it.nameWithoutExtension to PostchainUtils.xmlToGtv(it.readText())
                    }
                }
                .mapKeys {
                    NamedBlockchainRid(
                        it.key,
                        BlockchainRid(PostchainUtils.calcBlockchainRid(it.value).toByteArray())
                    )
                }
        }

        return if (name.isEmpty()) configsToAdd else configsToAdd.filter { name.contains(it.key.name) }
    }

    protected fun addSigners(gtvConfig: Gtv) =
        if (gtvConfig["signers"] != null) {
            gtvConfig
        } else {
            GtvFactory.gtv(
                *gtvConfig.asDict().toList().toTypedArray(),
                "signers" to GtvFactory.gtv(listOf(GtvFactory.gtv(nodeConfig.pubKeyByteArray)))
            )
        }

}