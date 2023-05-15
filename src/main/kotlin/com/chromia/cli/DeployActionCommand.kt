package com.chromia.cli

import com.chromia.cli.compile.config.BlockchainConfigHolder
import com.chromia.cli.util.pubkey
import com.chromia.directory1.model.BlockchainAction
import com.chromia.directory1.proposal.proposeBlockchainActionOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClientProvider
import net.postchain.client.core.PostchainQuery
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.transaction.TransactionBuilder


open class DeployActionCommand(
        private val action: BlockchainAction,
        help: String,
        clientProvider: PostchainClientProvider
) : AbstractDeploymentCommand(name = action.name, help = help, clientProvider) {

    private val description by option(help = "Description on why the blockchain is being acted on").default("")

    override fun TransactionBuilder.addDeploymentOperation(client: PostchainQuery, clientConfig: PostchainClientConfig, configHolder: BlockchainConfigHolder) {
        blockchain?.map {
            proposeBlockchainActionOperation(
                    clientConfig.pubkey.data,
                    deployModel.chains[it]!!,
                    action,
                    description
            )
        }
    }

    override fun beforeDeployment(deployedChains: Collection<BlockchainConfigHolder>) {
        deployedChains.forEach { (name, _, _) ->
            if (!deployModel.chains.containsKey(name)) throw PrintMessage("The action \"${this.action.name}\" of Blockchain $name cannot be done since it has not been deployed to network $target. Specify target blockchain rid in config.yml")
        }
    }
    
    override fun afterDeployment(deployedChains: Collection<BlockchainConfigHolder>) {}
}

class DeployResumeCommand(clientProvider: PostchainClientProvider = PostchainClientProviderImpl()) : DeployActionCommand(BlockchainAction.resume, "Starts a paused blockchain in a container", clientProvider)
class DeployPauseCommand(clientProvider: PostchainClientProvider = PostchainClientProviderImpl()) : DeployActionCommand(BlockchainAction.pause, "Pauses a blockchain in a container", clientProvider)