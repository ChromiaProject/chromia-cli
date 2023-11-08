package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.tools.config.chromiaModelOption
import com.chromia.cli.tools.env.CliktCliEnv
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.nodePropertiesOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.gtvml.GtvMLParser

class NodeCommand : NoOpCliktCommand(help = "Interact with a test node") {
    override fun aliases() = createAliases()
}


fun nodeCommands() = NodeCommand().subcommands(
        StartCommand(),
        UpdateCommand()
)

abstract class AbstractNodeCommand(help: String) : CliktCommand(help = help) {
    protected val settings by chromiaModelOption()
    protected val blockchainConfigs by option(
            "-bc", "--blockchain-config",
            help = "Manually specify which blockchain-configs to run"
    )
            .file(mustExist = true, canBeDir = false)
            .multiple()

    protected val name by option(help = "Only start specified blockchains (multiple)", metavar = "NAME")
            .multiple()

    private val overrides by option("-p", help = "Override any property value (usage: -p key=value)", metavar = "KEY=VALUE").associate()
    private val nodeConfigFile by nodePropertiesOption()
    protected val nodeConfig by lazy { nodeConfigFile ?: NodeConfig.getDefaultNodeConfig(settings.model, overrides) }

    protected fun extractConfigs(): Collection<ChromiaCompileResult> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            val blockchainsToCompile = settings.model.blockchains.filter { name.isEmpty() || name.contains(it.key) }.keys
            ChromiaCompileApi.compile(CliktCliEnv(this), settings.model, settings.projectFolder, blockchainsToCompile, filterModules = true)
        } else {
            blockchainConfigs
                    .filter { name.isEmpty() || name.contains(it.nameWithoutExtension) }
                    .associate {
                        //TODO remove GTV is no longer supported
                        if (it.extension == "gtv") {
                            it.inputStream().use { inputStream ->
                                it.nameWithoutExtension to GtvDecoder.decodeGtv(inputStream)
                            }
                        } else {
                            it.nameWithoutExtension to GtvMLParser.parseGtvML(it.readText())
                        }
                    }
                    .map { ChromiaCompileResult(it.key, it.value) }

        }

        return configsToAdd
    }
}
