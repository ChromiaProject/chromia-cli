package com.chromia.cli.command.deployment.voterset

import com.chromia.cli.command.deployment.proposal.formatThreshold
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.directory1.common.queries.getVoterSets
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.provideDelegate


class VotersetListCommand : CliktCommand(
        name = "list",
        help = """
            List all voter sets, 
            voterset that owns your container will be named; 
            container_<container id>_deployer
        """.trimIndent()
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)
        val voterSets = client.getVoterSets()
        echo(defaultTable {
            header { row("Name", "Governor", "Majority level") }
            body {
                voterSets.map {
                    row(it.name, it.gorvernor, formatThreshold(it.threshold))
                }
            }
        })
    }
}
