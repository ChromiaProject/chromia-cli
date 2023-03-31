package com.chromia.cli

import com.chromia.cli.compatibility.BlockchainOperations
import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.util.CliktClusterManagement
import com.chromia.cli.util.ClusterManagementFactory
import com.chromia.cli.util.HeightFinder
import com.chromia.cli.util.apiVersion
import com.chromia.cli.util.pubkey
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.client.core.PostchainClient
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.cm.cm_api.ClusterManagementImpl
import net.postchain.common.BlockchainRid

class DeployUpdateCommand(
        clientProvider: PostchainClientProvider = PostchainClientProviderImpl(),
        private val clusterManagementFactory: ClusterManagementFactory = Companion
) : AbstractDeploymentCommand(name = "update", help = "Update configuration of a blockchain", clientProvider) {
    private val height by option(help = "Deploy configuration at a specific height").long().validate {
        require(blockchain?.size == 1 || deployModel.chains.size == 1) { "When deploying to a specific height, only one blockchain can be updated at a time. use --blockchain flag to specify" }
    }

    override fun beforeDeployment(name: String, brid: BlockchainRid) {
        if (!deployModel.chains.containsKey(name)) throw PrintMessage("Blockchain $name cannot be updated since it has not been deployed to network $target. Specify target blockchain rid in config.yml")
    }

    override fun afterDeployment(deployedChains: Collection<BlockchainConfigHolder>) {}


    override fun addDeploymentOperation(transactionBuilder: TransactionBuilder, client: PostchainClient, configHolder: BlockchainConfigHolder) {
        val clusterManagement = clusterManagementFactory.buildClusterManagement(client)
        val heightChecker by lazy { HeightFinder(clientProvider, client.config, clusterManagement) }
        val blockchainRid = deployModel.chains[configHolder.name]!!
        BlockchainOperations(client.apiVersion, transactionBuilder, heightChecker)
                .proposeConfiguration(client.pubkey.data, blockchainRid, configHolder.configByteArray, clusterManagement.getClusterOfBlockchain(blockchainRid), height, true)
    }

    companion object : ClusterManagementFactory {
        override fun buildClusterManagement(client: PostchainClient) = CliktClusterManagement(ClusterManagementImpl(client))
    }
}
