package com.chromia.api.impl

import com.chromia.api.impl.deployment.awaitConfirmation
import com.chromia.api.impl.deployment.compressConfiguration
import com.chromia.api.impl.deployment.findBlockchainRid
import com.chromia.api.impl.deployment.postTransaction
import com.chromia.api.result.BlockchainConfiguration
import com.chromia.api.result.BlockchainDeploymentResult
import com.chromia.build.tools.HeightFinder
import com.chromia.build.tools.compatibility.BlockchainOperations
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.util.apiVersion
import com.chromia.cli.model.DeploymentModel
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.proposeBlockchainActionOperation
import java.util.stream.Collectors
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.tx.TransactionStatus
import net.postchain.d1.cluster.ClusterManagement
import net.postchain.rell.api.base.RellCliEnv


internal fun createNew(
        cliEnv: RellCliEnv,
        deploymentModel: DeploymentModel,
        config: ChromiaClientConfig,
        configurations: List<BlockchainConfiguration>,
        compressConfiguration: Boolean,
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
): List<BlockchainDeploymentResult> {
    require(deploymentModel.container != null) { "No container id is configured" }
    require(configurations.none { deploymentModel.chains.containsKey(it.name) }) { "Deployment for chain ${configurations.filter { it.name in deploymentModel.chains }.map { it.name }} already configured" }
    require(config.signers.isNotEmpty()) { "No signers configured" }

    val client = config.setDeployment(deploymentModel).client(clientProvider)

    val deploymentOperation: (TransactionBuilder, BlockchainConfiguration) -> Unit = { builder, bc ->
        BlockchainOperations(client.apiVersion, builder)
                .newBlockchainOperation(client.config.signers.first().pubKey.data, bc.configByteArray, bc.name, deploymentModel.container)
    }
    val deployedChains = configurations.parallelStream()
            .map { if (compressConfiguration) compressConfiguration(client, client.apiVersion, it) else it }
            .map { bc -> postTransaction(cliEnv, client, bc) { deploymentOperation(it, bc) } }
            .map { awaitConfirmation(cliEnv, client, it) }
            .map { findBlockchainRid(cliEnv, client, client.apiVersion, it) }
            .collect(Collectors.toList())
            .filterNotNull()

    if (deployedChains.size < configurations.size) {
        throw RuntimeException("Failed to deploy blockchains ${configurations.map { it.name }.filter { name -> name in deployedChains.map { it.blockchain.name } }}")
    }

    return deployedChains
}

internal fun updateExisting(
        cliEnv: RellCliEnv,
        deploymentModel: DeploymentModel,
        config: ChromiaClientConfig,
        configurations: List<BlockchainConfiguration>,
        height: Long?,
        compressConfigurations: Boolean,
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        clusterManagementFactory: ClusterManagementFactory = ClusterManagementFactory { ClusterManagementImpl(it) }
): List<BlockchainDeploymentResult> {
    require(height == null || configurations.size == 1) { "Cannot update multiple blockchains when height is set" }
    require(configurations.all { deploymentModel.chains.containsKey(it.name) }) { "Deployment for chain ${configurations.filterNot { it.name in deploymentModel.chains }.map { it.name }} not found" }
    require(config.signers.isNotEmpty()) { "No signers configured" }

    val client = config.setDeployment(deploymentModel).client(clientProvider)

    val clusterManagement = clusterManagementFactory(client)
    val heightChecker by lazy { HeightFinder(clientProvider, client.config, clusterManagement) }
    val deploymentOperation: (TransactionBuilder, BlockchainConfiguration) -> Unit = { builder, bc ->
        val blockchainRid = deploymentModel.chains[bc.name]!!
        BlockchainOperations(client.apiVersion, builder, heightChecker)
                .proposeConfiguration(client.config.signers.first().pubKey.data, blockchainRid, bc.configByteArray, clusterManagement.getClusterOfBlockchain(blockchainRid), height, true)
    }
    val deployedChains = configurations.parallelStream()
            .map { if (compressConfigurations) compressConfiguration(client, client.apiVersion, it) else it }
            .map { bc -> postTransaction(cliEnv, client, bc) { deploymentOperation(it, bc) } }
            .map { awaitConfirmation(cliEnv, client, it) }
            .map { it.copy(blockchainRid = deploymentModel.chains[it.blockchain.name]) }
            .collect(Collectors.toList())
            .filterNotNull()

    if (deployedChains.size < configurations.size) {
        throw RuntimeException("Failed to update blockchains ${configurations.map { it.name }.filter { name -> name in deployedChains.map { it.blockchain.name } }}")
    }

    return deployedChains
}

fun interface ClusterManagementFactory {
    operator fun invoke(client: PostchainClient): ClusterManagement
}


fun action(
        deploymentModel: DeploymentModel,
        config: ChromiaClientConfig,
        action: BlockchainAction,
        reason: String,
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl()
): Pair<Boolean, String?> {
    require(config.signers.isNotEmpty()) { "No signers configured" }
    val result = config.setDeployment(deploymentModel)
            .client(clientProvider)
            .transactionBuilder()
            .addNop()
            .apply {
                deploymentModel.chains.forEach { (_, brid) ->
                    proposeBlockchainActionOperation(
                            config.signers.first().pubKey.data,
                            brid,
                            action,
                            reason
                    )
                }
            }
            .sign()
            .postAwaitConfirmation()
    return (result.status == TransactionStatus.CONFIRMED) to result.rejectReason
}
