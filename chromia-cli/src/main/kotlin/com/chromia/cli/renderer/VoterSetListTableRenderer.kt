package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.voterset.VotersetListCommand
import com.chromia.cli.command.deployment.voterset.VotersetListRenderData
import com.chromia.cli.tools.formatter.defaultTable

class VoterSetListTableRenderer(val cliktCommand: VotersetListCommand) : Renderer<VotersetListRenderData> {
    override fun display(data: VotersetListRenderData) {
        cliktCommand.echo(cliktCommand.defaultTable {
            header { row("Name", "Governor", "Majority level") }
            body {
                data.votersets.map {
                    row(it.name, it.governor, it.majorityLevel)
                }
            }
        })
    }
}
