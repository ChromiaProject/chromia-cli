package com.chromia.cli.command.deployment

import com.chromia.api.ChromiaDeploymentApi
import com.chromia.api.filterChains
import com.chromia.build.tools.config.ChromiaPredefinedNetworks
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.DeploymentModel
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.util.blockchainOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.split
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.postchain.gtv.GtvFactory

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

    private val confirm by option(
            "-y",
            help = "Confirm that this will remove the blockchain permanently"
    ).flag(default = false)

    override fun run() {
        blockchain?.forEach { chain ->
            if (!deployModel.chains.containsKey(chain)) throw PrintMessage("The action \"${this.action.name}\" of Blockchain ${chain} cannot be done since it has not been deployed to network $target. Specify target blockchain rid in chromia.yml")
        }
        val deployModel = settings.model.deployments[target]!!
                .filterChains(blockchain)
                .populatePredefinedNetworkConfig(target)

        settings.config.configureSigners(keyPairSource)

        action.takeIf { it == BlockchainAction.remove }?.run {
            if (!confirm) {
                if (terminal.terminalInfo.inputInteractive) {
                    if (YesNoPrompt("Continuing with execution will delete blockchain '$blockchain' with brid: '${deployModel.blockchainRid}' on network '$target'.",
                                    terminal, default = false
                            ).ask() != true) throw PrintMessage("Operation aborted")
                } else {
                    throw CliktError("Please specify -y option to force removal")
                }
            }
        }

        val (res, msg) = ChromiaDeploymentApi.action(deployModel, settings.config, action, description)
        if (res) {
            echo("${action.name} of blockchain ${deployModel.chains.keys} was successful")
        } else {
            val altMsg = "${action.name} of blockchain ${deployModel.chains.keys} was unsuccessful"
            throw PrintMessage(msg ?: altMsg, statusCode = 1)
        }
        if (action == BlockchainAction.remove) {
            echo("INFO: Clean up deployment ${deployModel.chains.keys} from your config file under chains for network \"$target\", as it is no longer a valid deployment")
        }
    }
}

private fun DeploymentModel.populatePredefinedNetworkConfig(network: String): DeploymentModel {
    val resolvedBrid = blockchainRid ?: run {
        require(ChromiaPredefinedNetworks.isPredefined(network)) { "Network $network is not a predefined network" }
        ChromiaPredefinedNetworks.connect(network).directoryChainClient.config.blockchainRid
    }

    val resolvedUrl = if (!url.isNull()) {
        url
    } else {
        require(ChromiaPredefinedNetworks.isPredefined(network)) { "Network $network is not a predefined network" }
        GtvFactory.gtv(ChromiaPredefinedNetworks.connect(network).directoryChainApiUrls.map(GtvFactory::gtv))
    }

    return copy(blockchainRid = resolvedBrid, url = resolvedUrl)
}

class DeployResumeCommand : DeployActionCommand(BlockchainAction.resume, "Starts a paused blockchain")
class DeployPauseCommand : DeployActionCommand(BlockchainAction.pause, "Pauses a deployed blockchain")
class DeployRemoveCommand : DeployActionCommand(BlockchainAction.remove, "Removes a deployed blockchain (This action is permanent)")
