package com.chromia.cli.command.deployment

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.directory1.proposal_container.ContainerAction
import com.chromia.directory1.proposal_container.proposeContainerActionOperation
import com.chromia.directory1.proposal_container.resumeContainerOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.tx.TransactionStatus

sealed class ContainerActionCommand(
        name: String,
        help: String
) : ChromiaCommand(name, help) {

    protected val settings by chromiaModelConfigOption()
    protected val keyPairSource by keyPairSourceOption()
    protected val description by option(help = "Description on why the container is being acted on").default("")

    protected val target by deployTargetOption().required()
            .validate { require(settings.model.deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }
    protected val deployModel by lazy {
        val deployModel = settings.model.deployments[target]
        if (deployModel!!.container == null) throw PrintMessage("No container specified on network $target")
        deployModel
    }
}

class PauseContainerCommand : ContainerActionCommand(name = "pause", help = "Pause a container") {
    override fun run() {
        settings.config.configureSigners(keyPairSource)
        require(settings.config.signers.isNotEmpty()) { "No signers configured" }
        requireNotNull(deployModel.container) { "No container specified" }
        val result = settings.config.setDeployment(deployModel)
                .client(PostchainClientProviderImpl())
                .transactionBuilder()
                .proposeContainerActionOperation(
                        settings.config.signers.first().pubKey.data,
                        deployModel.container!!,
                        ContainerAction.pause,
                        description,
                )
                .addNop()
                .sign()
                .postAwaitConfirmation(txListener())
        if (result.status == TransactionStatus.CONFIRMED) {
            echo("Pause of container ${deployModel.container} was successful")
        } else {
            throw PrintMessage(result.rejectReason!!, statusCode = 1)
        }
    }
}

class ResumeContainerCommand : ContainerActionCommand(name = "resume", help = "Resume a paused container") {
    override fun run() {
        settings.config.configureSigners(keyPairSource)
        require(settings.config.signers.isNotEmpty()) { "No signers configured" }
        requireNotNull(deployModel.container) { "No container specified" }
        val result = settings.config.setDeployment(deployModel)
                .client(PostchainClientProviderImpl())
                .transactionBuilder()
                .resumeContainerOperation(
                        settings.config.signers.first().pubKey.data,
                        deployModel.container!!,
                )
                .addNop()
                .sign()
                .postAwaitConfirmation(txListener())
        if (result.status == TransactionStatus.CONFIRMED) {
            echo("Resume of container ${deployModel.container} was successful")
        } else {
            throw PrintMessage(result.rejectReason!!, statusCode = 1)
        }
    }
}
