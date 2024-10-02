package com.chromia.cli.renderer

import com.chromia.cli.command.deployment.proposal.ProposalInfoCommand
import com.chromia.cli.command.deployment.proposal.ProposalListCommand
import com.chromia.cli.command.deployment.voterset.VotersetInfoCommand
import com.chromia.cli.command.deployment.voterset.VotersetListCommand
import com.chromia.cli.util.TableOutputFormat
import com.chromia.directory1.model.Provider
import com.chromia.directory1.proposal.GetProposalResult
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import net.postchain.client.core.PostchainClient

abstract class AbstractRenderData

interface Renderer<T : AbstractRenderData> {
    fun display(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long, proposedBy: Provider) {}
    fun display(data: T) {}
}

object RendererFactory {
    @Suppress("UNCHECKED_CAST")
    fun <T : AbstractRenderData> createRenderer(outputFormat: TableOutputFormat?, cliktCommand: CliktCommand): Renderer<T> {

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

            is VotersetInfoCommand -> when (outputFormat) {
                TableOutputFormat.JSON -> VoterSetInfoJSONRenderer(cliktCommand)
                TableOutputFormat.table -> VoterSetInfoTableRenderer(cliktCommand)
                else -> if (cliktCommand.terminal.info.outputInteractive) VoterSetInfoTableRenderer(cliktCommand) else VoterSetInfoJSONRenderer(cliktCommand)
            }

            is VotersetListCommand -> when (outputFormat) {
                TableOutputFormat.JSON -> VoterSetListJSONRenderer(cliktCommand)
                TableOutputFormat.table -> VoterSetListTableRenderer(cliktCommand)
                else -> if (cliktCommand.terminal.info.outputInteractive) VoterSetListTableRenderer(cliktCommand) else VoterSetListJSONRenderer(cliktCommand)
            }

            else -> throw IllegalArgumentException("command missing renderer")
        } as Renderer<T>
    }
}