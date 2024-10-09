package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.renderer.AbstractRenderData
import com.chromia.cli.renderer.RendererFactory
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.common.queries.getVoterSets
import com.github.ajalt.clikt.parameters.groups.provideDelegate


class VotersetListCommand : ChromiaCommand(
        name = "list",
        help = """
            List all voter sets, 
            voterset that owns your container will be named; 
            container_<container id>_deployer
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val voterSets = client.getVoterSets()
        val render = RendererFactory.createRenderer<VotersetListRenderData>(outputFormat, this)
        render.display(VotersetListRenderData(
                voterSets.map {
                    votersetRendererData(it.name, it.gorvernor, formatThreshold(it.threshold))
                }
        ))
    }
}

data class VotersetListRenderData(val votersets: List<votersetRendererData>) : AbstractRenderData()
data class votersetRendererData(val name: String, val governor: String, val majorityLevel: String)