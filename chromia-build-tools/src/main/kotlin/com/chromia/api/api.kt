package com.chromia.api

import com.chromia.api.impl.compileGtv
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.cli.model.ChromiaModel
import net.postchain.common.types.WrappedByteArray
import net.postchain.rell.api.base.RellCliEnv
import java.nio.file.Path

object ChromiaCompileApi {

    /**
     * Builds blockchain configurations
     */
    fun build(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
            : List<BlockchainConfiguration> = compileGtv(cliEnv, model, projectDir)

    /**
     * Verifies rell source code and computes the RID
     */
    @ExperimentalApi
    fun verify(cliEnv: RellCliEnv, model: ChromiaModel, projectDir: Path)
            : WrappedByteArray = com.chromia.api.impl.verify(cliEnv, model, projectDir)
}
