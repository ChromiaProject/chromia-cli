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
import com.chromia.directory1.common.queries.getVoterSets
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option


class VotersetListCommand : ChromiaCommand(
        name = "list",
        help = " List all voter sets"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val outputFormat by tableOutputFormat()
    private val container by option("-c", "--container", help = """
        Specify the container to receive the voterset for,
        If not set it will default to all votersets.
    """.trimIndent()
    )

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val render = RendererFactory.createRenderer<VotersetListRenderData>(outputFormat, this)
        if (container != null) {
            val containerData = client.getContainerData(container!!)
            val voterSet = client.getVoterSetInfo(containerData.deployer)
            render.display(VotersetListRenderData(listOf(votersetRendererData(voterSet.name, voterSet.governor, formatThreshold(voterSet.threshold)))))
        } else {
            val voterSets = client.getVoterSets()

            render.display(VotersetListRenderData(
                    voterSets.map {
                        votersetRendererData(it.name, it.gorvernor, formatThreshold(it.threshold))
                    }
            ))
        }
    }
}

data class VotersetListRenderData(val votersets: List<votersetRendererData>) : AbstractRenderData()
data class votersetRendererData(val name: String, val governor: String, val majorityLevel: String)