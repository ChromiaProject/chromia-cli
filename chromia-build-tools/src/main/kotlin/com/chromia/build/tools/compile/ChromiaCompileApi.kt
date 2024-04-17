package com.chromia.build.tools.compile

import com.chromia.api.filterBlockchains
import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv
import java.io.File

@Deprecated("Use the new api instead", replaceWith = ReplaceWith("ChromiaCompileApi", imports = arrayOf("com.chromia.api.ChromiaCompileApi")))
object ChromiaCompileApi {
    @Deprecated("Use the new api instead")
    fun compile(cliEnv: RellCliEnv, model: ChromiaModel, projectFolder: File): Collection<ChromiaCompileResult> {
        @Suppress("DEPRECATION")
        return compile(cliEnv, model, projectFolder, model.blockchains.keys)
    }

    @Deprecated("Use the new api instead")
    fun compile(
            cliEnv: RellCliEnv,
            model: ChromiaModel,
            projectFolder: File,
            blockchains: Collection<String>,
            @Suppress("UNUSED_PARAMETER")
            filterModules: Boolean = false,
            @Suppress("UNUSED_PARAMETER")
            inMemoryIcmf: Boolean = false,
            @Suppress("UNUSED_PARAMETER")
            validateGtv: Boolean = false
    ): Collection<ChromiaCompileResult> {
        return com.chromia.api.ChromiaCompileApi.build(cliEnv, model.filterBlockchains(blockchains), projectFolder.toPath()).map { ChromiaCompileResult(it.name, it.config) }
                .onEach { (name, gtv) -> storeConfig(gtv, name, model.compile.targetFile(projectFolder).toPath()) }
    }
}
