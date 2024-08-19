package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.directory1.common.queries.getVoterSetInfo
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.postchain.client.core.PostchainClient

fun CliktCommand.nameOption(helpMessage: String) = option("-n", "--name", help = helpMessage)


class VotersetInfoCommand : CliktCommand(
        name = "info",
        help = "Show information of voter set"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val name by nameOption("Name of voter set").required()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        showVoterSetInfo(client, name)
    }
}

fun CliktCommand.showVoterSetInfo(client: PostchainClient, name: String) {
    val voterSet = client.getVoterSetInfo(name)
    echo(defaultTable {
        body {
            row("Voter set", voterSet.name)
            row("Governed by", voterSet.governor)
            row("Threshold", formatThreshold(voterSet.threshold))
            voterSet.members.forEachIndexed { index, bytes -> row("Member $index", bytes.toHex()) }
        }
    })
}
