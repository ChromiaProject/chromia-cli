package com.chromia.cli.command.deployment.proposal

import com.chromia.cli.util.TableOutputFormat
import com.chromia.directory1.model.Provider
import com.chromia.directory1.proposal.GetProposalResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import net.postchain.client.core.PostchainClient

abstract class Renderer {
    abstract fun display(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long, proposedBy: Provider)
}

object RendererFactory {
    fun createRenderer(outputFormat: TableOutputFormat?, cliktCommand: CliktCommand): Renderer {
        return when (outputFormat) {
            TableOutputFormat.JSON -> JSONRenderer(cliktCommand)
            TableOutputFormat.table -> TableRenderer(cliktCommand)
            else -> if (cliktCommand.terminal.info.outputInteractive) TableRenderer(cliktCommand) else JSONRenderer(cliktCommand)
        }
    }
}