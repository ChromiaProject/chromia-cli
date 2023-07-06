package com.chromia.cli

import com.chromia.cli.compile.NodeConfig
import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.build.tools.compile.ChromiaCompileApi
import com.chromia.cli.tools.launcher.createAliases
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.nodePropertiesOption
import com.chromia.cli.util.settingsOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.options.*
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

    protected fun extractConfigs(): Collection<ChromiaCompileResult> {
        val configsToAdd = if (blockchainConfigs.isEmpty()) {
            val blockchainsToCompile = settings.blockchains.filter { name.isEmpty() || name.contains(it.key) }.keys
            ChromiaCompileApi.compile(CliktCliEnv(this), settings.model, settings.file.parentFile, blockchainsToCompile)
        } else {
            blockchainConfigs
                    .filter { name.isEmpty() || name.contains(it.nameWithoutExtension) }
                    .associate {
                        if (it.extension == "gtv") {
                            it.inputStream().use { inputStream ->
                                it.nameWithoutExtension to GtvDecoder.decodeGtv(inputStream)
                            }
                        } else {
                            it.nameWithoutExtension to GtvMLParser.parseGtvML(it.readText())
                        }
                    }
                    .map { ChromiaCompileResult(it.key, it.value) }

        }.sortedBy { it.name }

        return configsToAdd
    }
}
