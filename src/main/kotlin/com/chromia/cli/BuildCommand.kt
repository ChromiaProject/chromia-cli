package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.BlockchainConfigurationWriter.storeConfig
import com.chromia.cli.exception.LibraryTamperedException
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.Settings
import com.chromia.cli.util.createAliases
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.showBridOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import net.postchain.rell.utils.cli.RellCliEnv
import java.nio.file.Files
import kotlin.io.path.exists
import kotlin.streams.toList


class BuildCommand : CliktCommand(help = "Build an application and create a blockchain configuration", invokeWithoutSubcommand = true) {
    private val showBrid by showBridOption()
    private val settings by settingsOption()
    override fun aliases() = createAliases()

    init {
        context { helpFormatter = CliktHelpFormatter(showDefaultValues = true) }
    }

    override fun run() {
        if (currentContext.invokedSubcommand != null) return
        compile(CliktCliEnv(this), settings).apply {
            if (showBrid) this.forEach { (name, brid, _) -> echo("$name $brid") }
        }
    }

    companion object {
        fun compile(cliEnv: RellCliEnv, settings: Settings): Collection<BlockchainConfigHolder> {

            settings.libs.forEach { (name, rellLibrary) ->
                run {
                    val libraryLocation = settings.target.toPath().resolve("libs").resolve(name)
                    if (libraryLocation.exists()) {
                        val files = Files.walk(libraryLocation).map { it.toFile() }.toList()
                        if (!rellLibrary.validateRid(files)) {
                            throw LibraryTamperedException(rellLibrary.rid.toString(), name)
                        }
                    } else {
                        throw PrintMessage("Library $name is not installed, install before building")
                    }
                }
            }


            return BlockchainConfigurationGenerator(cliEnv, settings.compile, settings.blockchains, settings.source)
                    .generate()
                    .onEach { (name, _, gtv) -> storeConfig(gtv, name, settings.target.toPath()) }

        }
    }

}
