package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.renderer.AbstractRenderData
import com.chromia.cli.renderer.RendererFactory
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.common.queries.getVoterSetInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.common.types.WrappedByteArray

fun CliktCommand.nameOption(helpMessage: String) = option("-n", "--name", help = helpMessage)


class VotersetInfoCommand : ChromiaCommand(
        name = "info",
        help = "Show information of voter set"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val name by nameOption("Name of voter set").required()
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val voterSet = client.getVoterSetInfo(name)
        val render = RendererFactory.createRenderer<VotersetInfoRenderData>(outputFormat, this)
        render.display(VotersetInfoRenderData(voterSet.name, voterSet.governor, formatThreshold(voterSet.threshold), voterSet.members))
    }
}

data class VotersetInfoRenderData(val voterset: String, val governor: String, val threshold: String, val members: List<WrappedByteArray>) : AbstractRenderData()