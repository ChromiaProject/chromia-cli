package com.chromia.api

import com.chromia.api.impl.compileGtv
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.lib.RepositoryCloner
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.DeploymentModel
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.rell.api.base.RellCliEnv

object ChromiaCompileApi {

    /**
     * Builds blockchain configurations
     */
    fun build(cliEnv: RellCliEnv, model: ChromiaModel)
            : List<BlockchainConfiguration> = compileGtv(cliEnv, model)

    /**
     * Verifies rell source code and computes the RID
     */
    @ExperimentalApi
    fun verify(cliEnv: RellCliEnv, model: ChromiaModel)
            : Boolean = com.chromia.api.impl.verify(cliEnv, model)
}

object ChromiaLibrariesApi {
    /**
     * Installs libraries to src/lib folder.
     */
    @ExperimentalApi("May want to remove RepositoryCloner parameter from this api")
    fun install(cliEnv: RellCliEnv, model: ChromiaModel, repositoryCloner: RepositoryCloner)
        = com.chromia.api.impl.install(cliEnv, repositoryCloner, model)
}

object ChromiaDeploymentApi {
    fun create(cliEnv: RellCliEnv, model: DeploymentModel, chromiaConfig: ChromiaClientConfig, configurations: List<BlockchainConfiguration>, compressConfigirations: Boolean): List<BlockchainDeploymentResult> {
            return com.chromia.api.impl.createNew(cliEnv, model, chromiaConfig, configurations, compressConfigirations, PostchainClientProviderImpl())
    }

    fun update(cliEnv: RellCliEnv, model: DeploymentModel, chromiaConfig: ChromiaClientConfig, configurations: List<BlockchainConfiguration>, compressConfigurations: Boolean, height: Long? = null): List<BlockchainDeploymentResult> {
        return com.chromia.api.impl.updateExisting(cliEnv, model, chromiaConfig, configurations, height, compressConfigurations, PostchainClientProviderImpl()) { ClusterManagementImpl(it) }
    }

    fun action(model: DeploymentModel, chromiaConfig: ChromiaClientConfig, action: BlockchainAction, reason: String): Pair<Boolean, String?> {
        return com.chromia.api.impl.action(model, chromiaConfig, action, reason)
    }
}
