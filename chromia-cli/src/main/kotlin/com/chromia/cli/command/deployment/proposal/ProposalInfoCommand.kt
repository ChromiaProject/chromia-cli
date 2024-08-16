package com.chromia.cli.command.deployment.proposal

import com.chromia.build.tools.util.apiVersion
import com.chromia.cli.command.deployment.proposal.gtv.diff.GtvDiffFinder
import com.chromia.cli.d1.NopAnchoringGTXModule.Companion.cryptoSystem
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.cli.util.DeployedNetworkOption
import com.chromia.directory1.common.queries.getProviderData
import com.chromia.directory1.proposal.GetProposalResult
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.ProposalVotingResults
import com.chromia.directory1.proposal.getProposal
import com.chromia.directory1.proposal.getProposalVoterInfo
import com.chromia.directory1.proposal.getProposalVotingResults
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.getBlockchainActionProposal
import com.chromia.directory1.proposal_blockchain.getBlockchainProposal
import com.chromia.directory1.proposal_blockchain.getBlockchainUnarchiveActionProposal
import com.chromia.directory1.proposal_blockchain.getConfigurationProposal
import com.chromia.directory1.proposal_blockchain.getConfigurationProposalAt
import com.chromia.directory1.proposal_blockchain.getProposedForcedConfiguration
import com.chromia.directory1.proposal_blockchain_move.getBlockchainMoveFinishProposal
import com.chromia.directory1.proposal_blockchain_move.getBlockchainMoveProposal
import com.chromia.directory1.proposal_voter_set.getVoterSetUpdateProposal
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.mordant.table.SectionBuilder
import java.time.Instant
import java.util.Date
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId
import net.postchain.common.types.WrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvFactory
import net.postchain.gtv.merkle.GtvMerkleHashCalculator
import net.postchain.gtv.merkleHash

private val AUTO_GENERATED_CONFIG_FIELDS = setOf(
        "chain0_last_block_rid",
        "chain0_block_height",
        "chain0_tx_rid",
        "chain0_op_index"
)

class ProposalInfoCommand
    : CliktCommand(
        name = "info",
        help = "Get information of a given proposal"
) {
    private val settings by optionalChromiaModelConfigOption()
    private val networkTarget by DeployedNetworkOption { settings.model ?: ChromiaModel.default() }
    private val idx by proposalIndexOption().convert { RowId(it) }.required()

    override fun run() {
        val clientConfig = settings.config.setApiUrls(networkTarget.url).setBrid(networkTarget.brid)
        val client = networkTarget.createClient(clientConfig)

        showProposalInfo(client, idx)
    }
}

fun CliktCommand.showProposalInfo(client: PostchainClient, id: RowId?) {
    val proposal = client.getProposal(id) ?: return echo("Proposal $id not found")
    val proposedBy = client.getProviderData(PubKey(proposal.proposedBy))
    val apiVersion = client.apiVersion

    echo(defaultTable {
        body {
            printProposalHeader(proposal.id, proposal.type, proposal.timestamp, proposedBy.pubkey, proposedBy.name)
            row("State", proposal.state.toString())
            printVotingInfo(client, proposal, apiVersion)
            row("Description", proposal.description)
        }
    })

    if (proposal.state == ProposalState.PENDING) {
        if (terminal.info.outputInteractive) echo("Proposal details")
        echo(formatPendingProposal(apiVersion, client, proposal.id, proposal.type))
    }
}

private fun SectionBuilder.printVotingInfo(client: PostchainClient, proposal: GetProposalResult, apiVersion: Long) {
    if (proposal.state == ProposalState.PENDING) {
        printVotingResults(client.getProposalVotingResults(proposal.id))
    }

    if (apiVersion >= 9) {
        val votingInfo = client.getProposalVoterInfo(proposal.id)
        row("Providers that accepted", votingInfo.filter { it.vote }.joinToString("\n") { formatProvider(it.provider, it.providerName) })
        row("Providers that rejected", votingInfo.filterNot { it.vote }.joinToString("\n") { formatProvider(it.provider, it.providerName) })
    }
}

private fun SectionBuilder.printVotingResults(votingResults: ProposalVotingResults) {
    row("Positive votes", votingResults.positiveVotes.toString())
    row("Negative votes", votingResults.negativeVotes.toString())
    row("Max votes", votingResults.maxVotes.toString())
    row("Threshold", formatThreshold(votingResults.threshold))
    row("Status", votingResults.votingResult.toString())
}

fun formatThreshold(threshold: Long): String {
    return when (threshold) {
        0L -> "super majority (>66.66%)"
        -1L -> "majority (>50%)"
        else -> threshold.toString()
    }
}

private fun SectionBuilder.printProposalHeader(id: RowId, type: ProposalType, timestamp: Long, proposedByPubkey: WrappedByteArray, proposedByName: String) {
    row("Proposal", "${id.id} - ${type.name}")
    row("Proposed by", formatProvider(proposedByPubkey, proposedByName))
    row("Time", "${Date.from(Instant.ofEpochMilli(timestamp))}")
}

private fun formatProvider(providerPubKey: WrappedByteArray, providerName: String) =
        "${providerPubKey.toHex()}${if (providerName.isNotEmpty()) " - $providerName" else ""}"

private fun CliktCommand.formatPendingProposal(apiVersion: Long, client: PostchainClient, proposalId: RowId, proposalType: ProposalType): Any {
    return when (proposalType) {
        ProposalType.bc -> {
            val bp = client.getBlockchainProposal(proposalId) ?: return ""
            "Container: ${bp.container}\nConfig hash: ${getDataHash(bp.data)}"
        }

        ProposalType.configuration -> {
            val p = client.getConfigurationProposal(proposalId) ?: return ""
            val currentConf = pruneAutoGeneratedConfigFields(GtvDecoder.decodeGtv(p.currentConf.data.data) as GtvDictionary)
            val newConf = GtvDecoder.decodeGtv(p.proposedConf.data.data) as GtvDictionary
            "Proposed configuration:\n\n${GtvDiffFinder.diff(currentConf, newConf).diff}"
        }

        ProposalType.configuration_at -> {
            val p = client.getConfigurationProposalAt(proposalId) ?: return ""
            val currentConf = pruneAutoGeneratedConfigFields(GtvDecoder.decodeGtv(p.currentConf.data.data) as GtvDictionary)
            val newConf = GtvDecoder.decodeGtv(p.proposedConf.data.data) as GtvDictionary
            "Enabled at height: ${p.proposedConf.height}\n\n${GtvDiffFinder.diff(currentConf, newConf).diff}"
        }

        ProposalType.force_configuration -> {
            val p = client.getProposedForcedConfiguration(proposalId) ?: return ""
            val currentConf = pruneAutoGeneratedConfigFields(GtvDecoder.decodeGtv(p.currentConf.data.data) as GtvDictionary)
            val newConf = GtvDecoder.decodeGtv(p.forcedConf.configData.data) as GtvDictionary
            "Force at height: ${p.forcedConf.height}\n\n${GtvDiffFinder.diff(currentConf, newConf).diff}"
        }

        ProposalType.voter_set_update -> {
            val vsu = client.getVoterSetUpdateProposal(proposalId.id) ?: return ""
            return defaultTable {
                body {
                    row("Voter set", vsu.voterSet)
                    row("Governor update", vsu.governor ?: "")
                    row("Majority threshold update", vsu.threshold?.toString() ?: "")
                    row("New member", vsu.addMember.joinToString(", ") { it.toHex() })
                    row("Remove member", vsu.removeMember.joinToString(", ") { it.toHex() })
                }
            }
        }

        ProposalType.blockchain_action -> {
            val pba = client.getBlockchainActionProposal(proposalId) ?: return ""
            return defaultTable {
                body {
                    row("Blockchain RID", pba.blockchain.toHex())
                    row("Blockchain name", pba.blockchainName)
                    row("Action", pba.action.name)

                    if (apiVersion >= 33 && pba.action == BlockchainAction.unarchive) {
                        val unarchivingProposal = client.getBlockchainUnarchiveActionProposal(proposalId)
                        if (unarchivingProposal != null) {
                            row("Source container", unarchivingProposal.sourceContainer)
                            row("Destination container", unarchivingProposal.destinationContainer)
                            row("Final height", unarchivingProposal.finalHeight)
                        }
                    }
                }
            }
        }

        ProposalType.blockchain_move_start -> {
            when {
                apiVersion >= 33 -> {
                    val proposal = client.getBlockchainMoveProposal(proposalId) ?: return ""
                    return defaultTable {
                        body {
                            row("Blockchain RID", proposal.blockchainRid)
                            row("Blockchain name", proposal.blockchainName)
                            row("Cluster", proposal.cluster)
                            row("Container", proposal.container)
                        }
                    }
                }

                else -> return ""
            }
        }

        ProposalType.blockchain_move_cancel -> {
//            val proposal = client.getBlockchainMoveCancelProposal(proposalId) ?: return ""
//            return "$proposal"
            return "Not yet implemented"
        }

        ProposalType.blockchain_move_finish -> {
            when {
                apiVersion >= 33 -> {
                    val proposal = client.getBlockchainMoveFinishProposal(proposalId) ?: return ""
                    return defaultTable {
                        body {
                            row("Blockchain RID", proposal.blockchainRid)
                            row("Blockchain name", proposal.blockchainName)
                            row("Cluster", proposal.cluster)
                            row("Container", proposal.container)
                            row("Final height", proposal.finalHeight)
                        }
                    }
                }

                else -> return ""
            }
        }

        else -> {
            return "Printing output information for proposal type: $proposalType is not supported yet"
        }
    }
}

private fun getDataHash(configData: WrappedByteArray) = GtvDecoder.decodeGtv(configData.data)
        .merkleHash(GtvMerkleHashCalculator(cryptoSystem))
        .wrap()

private fun pruneAutoGeneratedConfigFields(config: GtvDictionary): GtvDictionary =
        GtvFactory.gtv(config.asDict().filterKeys { !AUTO_GENERATED_CONFIG_FIELDS.contains(it) })

fun CliktCommand.proposalIndexOption() = option("--id", help = "Id of the proposal").long()

