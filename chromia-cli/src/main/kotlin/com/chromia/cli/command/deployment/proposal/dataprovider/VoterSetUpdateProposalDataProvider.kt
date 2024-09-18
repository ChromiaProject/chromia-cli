package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.cli.tools.formatter.defaultTable
import com.chromia.directory1.proposal_voter_set.GetVoterSetUpdateProposalResult
import com.chromia.directory1.proposal_voter_set.getVoterSetUpdateProposal
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId

class VoterSetUpdateProposalDataProvider : ProposalDataProvider<GetVoterSetUpdateProposalResult> {

    override fun formatData(data: GetVoterSetUpdateProposalResult, cliktCommand: CliktCommand): Any {
        return cliktCommand.defaultTable {
            body {
                row("Voter set", data.voterSet)
                row("Governor update", data.governor ?: "")
                row("Majority threshold update", data.threshold?.toString() ?: "")
                row("New member", data.addMember.joinToString(", ") { it.toHex() })
                row("Remove member", data.removeMember.joinToString(", ") { it.toHex() })
            }
        }
    }

    override fun getData(client: PostchainClient, proposalId: RowId): GetVoterSetUpdateProposalResult? {
        return client.getVoterSetUpdateProposal(proposalId.id)
    }
}