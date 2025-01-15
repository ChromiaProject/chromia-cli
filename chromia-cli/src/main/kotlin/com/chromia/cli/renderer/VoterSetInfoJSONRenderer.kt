package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.voterset.VotersetInfoCommand
import com.chromia.cli.command.deployment.voterset.VotersetInfoRenderData
import com.chromia.cli.tools.formatter.json

class VoterSetInfoJSONRenderer(val cliktCommand: VotersetInfoCommand) : Renderer<VotersetInfoRenderData> {

    data class Member(val label: String, val memberId: String)

    override fun display(data: VotersetInfoRenderData) {
        val result = mutableMapOf<String, Any?>()
        result["Voter set"] = data.voterset
        result["Governed by"] = data.governor
        result["Threshold"] = data.threshold
        result["members"] = data.members.mapIndexed { index, bytes -> Member("Member $index", bytes.toHex()) }
        cliktCommand.echo(json(result))
    }
}