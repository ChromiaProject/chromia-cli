package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.filterChains
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.configureSigners
import com.chromia.cli.util.deployTargetOption
import com.chromia.cli.util.keyPairSourceOption
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.*

sealed class DeployActionCommand(
        private val action: BlockchainAction,
        help: String
) : ChromiaCommand(name = action.name, help = help) {

    protected val settings by chromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()
    private val description by option(help = "Description on why the blockchain is being acted on").default("")

    protected val target by deployTargetOption().required()
            .validate { require(settings.model.deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    protected val blockchain by blockchainOption(help = "Name of blockchain to deploy").split(",")
            .validate { require(settings.model.blockchains.keys.containsAll(it)) { "Specified blockchain(s) $it does not exist" } }
    protected val deployModel by lazy {
        val deployModel = settings.model.deployments[target]
        if (deployModel!!.container == null) throw PrintMessage("No container specified on network $target")
        deployModel
    }

    override fun run() {
        blockchain?.forEach { chain ->
            if (!deployModel.chains.containsKey(chain)) throw PrintMessage("The action \"${this.action.name}\" of Blockchain ${chain} cannot be done since it has not been deployed to network $target. Specify target blockchain rid in chromia.yml")
        }
        val deployModel = settings.model.deployments[target]!!.filterChains(blockchain)

        settings.config.configureSigners(keyPairSource)
        val (res, msg) = ChromiaDeploymentApi.action(deployModel, settings.config, action, description)
        if (res) {
            echo("${action.name} of blockchain ${deployModel.chains.keys} was successful")
        } else {
            throw PrintMessage(msg!!, statusCode = 1)
        }
        if (action == BlockchainAction.remove) {
            echo("INFO: Clean up deployment ${deployModel.chains.keys} from your config file under chains for network \"$target\", as it is no longer a valid deployment")
        }
    }
}

class DeployResumeCommand : DeployActionCommand(BlockchainAction.resume, "Starts a paused blockchain in a container")
class DeployPauseCommand : DeployActionCommand(BlockchainAction.pause, "Pauses a blockchain in a container")
class DeployRemoveCommand : DeployActionCommand(BlockchainAction.remove, "Removes a blockchain in a container (This action is permanent)")
