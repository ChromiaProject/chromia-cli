package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.compile.config.BlockchainConfigurationGenerator
import com.chromia.cli.compile.config.BlockchainConfigurationWriter.storeConfig
import com.chromia.cli.lib.LibraryMismatchException
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.CompileModel
import com.chromia.cli.model.RellLibraryModel
import com.chromia.cli.util.CliktCliEnv
import com.chromia.cli.util.createAliases
import com.chromia.cli.util.settingsOption
import com.chromia.cli.util.showBridOption
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.output.CliktHelpFormatter
import net.postchain.rell.api.base.RellCliEnv
import java.io.File
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
        compile(CliktCliEnv(this), settings.source, settings.target, settings.compile, settings.blockchains, settings.libs).apply {
            if (showBrid) this.forEach { (name, brid, _) -> echo("$name $brid") }
        }
    }

    companion object {
        fun compile(cliEnv: RellCliEnv, source: File, target: File, compileModel: CompileModel, blockchains: Map<String, BlockchainModel>, libs: Map<String, RellLibraryModel> = mapOf()): Collection<BlockchainConfigHolder> {

            //TODO make it so you can specify a lib to install
            //TODO create a file that is called lib installer, should be aligned with what is in installCommand
            libs.forEach { (name, rellLibrary) ->
                run {
                    val libraryLocation = target.toPath().resolve("libs").resolve(name)
                    if (libraryLocation.exists()) {
                        val files = Files.walk(libraryLocation).map { it.toFile() }.toList()
                        val (isValid, rid) = rellLibrary.isValid(files)
                        if (!isValid) {
                            throw LibraryMismatchException(rellLibrary.rid?.toHex() ?: "", rid?.toHex() ?: "", name)
                        }
                    } else {
                        throw PrintMessage("Library $name is not installed, install before building")
                    }
                }
            }


            return BlockchainConfigurationGenerator(cliEnv, compileModel, blockchains, source)
                    .generate()
                    .onEach { (name, _, gtv) -> storeConfig(gtv, name, target.toPath()) }

        }
    }

}
