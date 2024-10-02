package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.voterset.VotersetInfoCommand
import com.chromia.cli.command.deployment.voterset.VotersetInfoRenderData
import com.chromia.cli.tools.formatter.defaultTable

class VoterSetInfoTableRenderer(val cliktCommand: VotersetInfoCommand) : Renderer<VotersetInfoRenderData> {
    override fun display(data: VotersetInfoRenderData) {
        cliktCommand.echo(cliktCommand.defaultTable {
            body {
                row("Voter set", data.voterset)
                row("Governed by", data.governor)
                row("Threshold", data.threshold)
                data.members.forEachIndexed { index, bytes -> row("Member $index", bytes.toHex()) }
            }
        })
    }
}
