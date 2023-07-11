package com.chromia.build.tools.compile

import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import com.chromia.build.tools.lib.LibraryVerifyer
import com.chromia.cli.model.ChromiaModel
import java.io.File
import net.postchain.rell.api.base.RellCliEnv

object ChromiaCompileApi {
    fun compile(cliEnv: RellCliEnv, model: ChromiaModel, projectFolder: File): Collection<ChromiaCompileResult> {
        return compile(cliEnv, model, projectFolder, model.blockchains.keys)
    }

    fun compile(cliEnv: RellCliEnv, model: ChromiaModel, projectFolder: File, blockchains: Collection<String>): Collection<ChromiaCompileResult> {
        val libraryVerifyer = LibraryVerifyer(cliEnv)
        libraryVerifyer.verifyLibs(model.compile.sourceFile(projectFolder), model.libs)

        if (!model.blockchains.keys.containsAll(blockchains)) throw ValidationException("Cannot compile blockchains $blockchains. Configured chains are ${model.blockchains.keys}")
        return BlockchainConfigurationGenerator(cliEnv, model.compile, model.blockchains.filter { blockchains.contains(it.key) }, projectFolder)
                .generate()
                .onEach { (name, gtv) -> storeConfig(gtv, name, model.compile.targetFile(projectFolder).toPath()) }
    }
}
