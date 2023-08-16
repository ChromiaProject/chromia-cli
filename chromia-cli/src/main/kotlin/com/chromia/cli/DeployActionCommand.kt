package com.chromia.cli

import com.chromia.build.tools.compile.ChromiaCompileResult
import com.chromia.cli.util.pubkey
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.proposeBlockchainActionOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid


open class DeployActionCommand(
        private val action: BlockchainAction,
        help: String,
        clientProvider: PostchainClientProvider
) : AbstractDeploymentCommand(name = action.name, help = help, clientProvider) {

    private val description by option(help = "Description on why the blockchain is being acted on").default("")

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: ChromiaCompileResult) {
        blockchain?.map {
            proposeBlockchainActionOperation(
                    clientConfig.pubkey.data,
                    deployModel.chains[it]!!,
                    action,
                    description
            )
        }
    }

    override fun beforeDeployment(deployedChains: Collection<String>) {
        deployedChains.forEach { name ->
            if (!deployModel.chains.containsKey(name)) throw PrintMessage("The action \"${this.action.name}\" of Blockchain $name cannot be done since it has not been deployed to network $target. Specify target blockchain rid in chromia.yml")
        }
    }
    
    override fun afterDeployment(deployedChains: List<Pair<String, BlockchainRid>>) {}
}

class DeployResumeCommand(clientProvider: PostchainClientProvider = PostchainClientProviderImpl()) : DeployActionCommand(BlockchainAction.resume, "Starts a paused blockchain in a container", clientProvider)
class DeployPauseCommand(clientProvider: PostchainClientProvider = PostchainClientProviderImpl()) : DeployActionCommand(BlockchainAction.pause, "Pauses a blockchain in a container", clientProvider)