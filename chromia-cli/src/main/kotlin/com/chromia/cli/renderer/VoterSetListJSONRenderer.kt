package com.chromia.cli.renderer

import com.chromia.cli.base.formatter.json
import com.chromia.cli.command.deployment.voterset.VotersetListCommand
import com.chromia.cli.command.deployment.voterset.VotersetListRenderData

class VoterSetListJSONRenderer(val cliktCommand: VotersetListCommand) : Renderer<VotersetListRenderData> {
    override fun display(data: VotersetListRenderData) {
        val result = mutableMapOf<String, Any?>()
        data.votersets.map { voterset ->
            result[voterset.name] = voterset
        }
        cliktCommand.echo(json(result))
    }
}
