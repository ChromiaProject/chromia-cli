package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.renderer.AbstractRenderData
import com.chromia.cli.renderer.RendererFactory
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.common.queries.getContainerData
import com.chromia.directory1.common.queries.getVoterSetInfo
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import net.postchain.common.types.WrappedByteArray

class VotersetInfoCommand : ChromiaCommand(
        name = "info",
        help = "Show information of voter set"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }

    private val identifier: Identifier by mutuallyExclusiveOptions(
            option1 = option("-n", "--name", help = "Name of voter set")
                    .convert { Identifier.Voterset(it) },
            option2 = option("-c", "--container", help = "Name of the container that is governed by the voterset")
                    .convert { Identifier.Container(it) }
    )
            .single()
            .required()


    private val outputFormat by tableOutputFormat()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val voterSet = when (identifier) {
            is Identifier.Container -> {
                val containerName = (identifier as Identifier.Container).name
                val container = client.getContainerData(containerName)
                client.getVoterSetInfo(container.deployer)
            }

            is Identifier.Voterset -> {
                val name = (identifier as Identifier.Voterset).name
                client.getVoterSetInfo(name)
            }
        }

        val render = RendererFactory.createRenderer<VotersetInfoRenderData>(outputFormat, this)
        render.display(VotersetInfoRenderData(voterSet.name, voterSet.governor, formatThreshold(voterSet.threshold), voterSet.members))
    }
}

data class VotersetInfoRenderData(val voterset: String, val governor: String, val threshold: String, val members: List<WrappedByteArray>) : AbstractRenderData()

sealed class Identifier {
    data class Container(val name: String) : Identifier()
    data class Voterset(val name: String) : Identifier()
}