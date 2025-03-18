package com.chromia.cli.command.deployment.proposal

import com.chromia.build.tools.util.apiVersion
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.renderer.AbstractRenderData
import com.chromia.cli.renderer.RendererFactory
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.cli.util.tableOutputFormat
import com.chromia.directory1.common.queries.getProviderData
import com.chromia.directory1.proposal.getProposal
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import net.postchain.common.types.RowId
import net.postchain.crypto.PubKey

class ProposalInfoCommand
    : ChromiaCommand(
        name = "info",
        help = "Get information of a given proposal"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val idx by proposalIndexOption().convert { RowId(it) }.required()
    private val outputFormat by tableOutputFormat()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.urls).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        val proposal = client.getProposal(idx) ?: return echo("Proposal $idx not found")
        val proposedBy = client.getProviderData(PubKey(proposal.proposedBy))
        val apiVersion = client.apiVersion
        val render = RendererFactory.createRenderer<AbstractRenderData>(outputFormat, this)

        render.display(client, proposal, apiVersion, proposedBy)
    }
}

fun CliktCommand.proposalIndexOption() = option("--id", help = "Id of the proposal").long()
