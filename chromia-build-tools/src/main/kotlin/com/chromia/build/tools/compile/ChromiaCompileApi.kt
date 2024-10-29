package com.chromia.build.tools.compile

import com.chromia.api.filterBlockchains
import com.chromia.build.tools.compile.BlockchainConfigurationWriter.storeConfig
import com.chromia.cli.model.ChromiaModel
import net.postchain.rell.api.base.RellCliEnv
import java.io.File

@Deprecated("Use the new api instead", replaceWith = ReplaceWith("ChromiaCompileApi", imports = arrayOf("com.chromia.api.ChromiaCompileApi")))
@Suppress("DEPRECATION")
object ChromiaCompileApi {
    @Deprecated("Use the new api instead",
            ReplaceWith("ChromiaCompileApi.build(cliEnv, model)", "com.chromia.api.ChromiaCompileApi"))
    fun compile(cliEnv: RellCliEnv, model: ChromiaModel, projectFolder: File): Collection<ChromiaCompileResult> {
        return compile(cliEnv, model, projectFolder, model.blockchains.keys)
    }

    @Deprecated("Use the new api instead",
            ReplaceWith("ChromiaCompileApi.build(cliEnv, model.filterBlockchains(blockchains))", "com.chromia.api.ChromiaCompileApi"))
    fun compile(
            cliEnv: RellCliEnv,
            model: ChromiaModel,
            @Suppress("UNUSED_PARAMETER")
            projectFolder: File,
            blockchains: Collection<String>,
            @Suppress("UNUSED_PARAMETER")
            filterModules: Boolean = false,
            @Suppress("UNUSED_PARAMETER")
            inMemoryIcmf: Boolean = false,
            @Suppress("UNUSED_PARAMETER")
            validateGtv: Boolean = false
    ): Collection<ChromiaCompileResult> {
        return com.chromia.api.ChromiaCompileApi.build(cliEnv, model.filterBlockchains(blockchains)).map { ChromiaCompileResult(it.name, it.config) }
                .onEach { (name, gtv) -> storeConfig(gtv, name, model.compile.target) }
    }
}
