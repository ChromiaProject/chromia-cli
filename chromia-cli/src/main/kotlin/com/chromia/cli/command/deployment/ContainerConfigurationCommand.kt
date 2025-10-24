package com.chromia.cli.command.deployment

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.voterset.proposalDescriptionOption
import com.chromia.cli.tools.config.chromiaModelConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.util.slowDBStatementLogMsOption
import com.chromia.cli.util.deployTargetOption
import com.chromia.directory1.proposal_container.proposal_container_configuration.ContainerConfigurationData
import com.chromia.directory1.proposal_container.proposal_container_configuration.proposeContainerConfigurationOperation
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.common.tx.TransactionStatus

class ContainerConfigurationCommand : ChromiaCommand(
    name = "configuration",
    help = """
        Propose configurations for given container
        
        This command allows you to configure various container settings such as
        slow database statement logging thresholds.
    """.trimIndent()
) {

    private val settings by chromiaModelConfigOption()
    private val keyPairSource by keyPairSourceOption()

    private val slowDbStatementLogMs by slowDBStatementLogMsOption()

    private val description by proposalDescriptionOption {
        "Update container configuration for ${deployModel.container} - slow-db-statement-log-ms: $slowDbStatementLogMs"
    }

    private val target by deployTargetOption().required()
        .validate { require(settings.model.deployments.keys.contains(it)) { "Specified target [$it] does not exist" } }

    private val deployModel by lazy { settings.model.deployments[target]!! }

    override fun run() {
        settings.config.configureSigners(keyPairSource)
        require(settings.config.signers.isNotEmpty()) { "No signers configured" }
        requireNotNull(deployModel.container) {
            "No container specified, please update your chromia.yml file accordingly"
        }
        val result = settings.config.setDeployment(deployModel)
            .client(PostchainClientProviderImpl())
            .transactionBuilder()
            .proposeContainerConfigurationOperation(
                settings.config.signers.first().pubKey.data,
                deployModel.container!!,
                ContainerConfigurationData(slowDbStatementLogMs),
                description
            )
            .addNop()
            .sign()
            .postAwaitConfirmation(txListener())

        if (result.status == TransactionStatus.CONFIRMED) {
            echo("Container configuration proposal submitted with description: $description")
        } else {
            throw PrintMessage(result.rejectReason!!, statusCode = 1)
        }
    }
}
