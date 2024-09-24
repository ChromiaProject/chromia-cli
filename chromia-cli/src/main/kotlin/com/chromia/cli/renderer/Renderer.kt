package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.ProposalInfoCommand
import com.chromia.cli.command.deployment.proposal.ProposalListCommand
import com.chromia.cli.command.deployment.proposal.ProposalListRenderData
import com.chromia.cli.util.TableOutputFormat
import com.chromia.directory1.model.Provider
import com.chromia.directory1.proposal.GetProposalResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import net.postchain.client.core.PostchainClient

abstract class RenderData

interface Renderer<T : RenderData> {
    fun display(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long, proposedBy: Provider) {}
    fun display(data: ProposalListRenderData) {}
}

object RendererFactory {
    fun <T : RenderData> createRenderer(outputFormat: TableOutputFormat?, cliktCommand: CliktCommand): Renderer<out RenderData> {

        return when (cliktCommand) {
            is ProposalInfoCommand -> when (outputFormat) {
                TableOutputFormat.JSON -> ProposalInfoJSONRenderer(cliktCommand)
                TableOutputFormat.table -> ProposalInfoTableRenderer(cliktCommand)
                else -> if (cliktCommand.terminal.info.outputInteractive) ProposalInfoTableRenderer(cliktCommand) else ProposalInfoJSONRenderer(cliktCommand)
            }

            is ProposalListCommand -> when (outputFormat) {
                TableOutputFormat.JSON -> ProposalListJSONRenderer(cliktCommand)
                TableOutputFormat.table -> ProposalListTableRenderer(cliktCommand)
                else -> if (cliktCommand.terminal.info.outputInteractive) ProposalListTableRenderer(cliktCommand) else ProposalListJSONRenderer(cliktCommand)
            }

            else -> throw IllegalArgumentException("command missing renderer")
        }
    }
}