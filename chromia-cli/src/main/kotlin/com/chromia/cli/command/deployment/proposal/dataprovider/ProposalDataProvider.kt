package com.chromia.cli.command.deployment.proposal.dataprovider

import com.chromia.directory1.proposal.ProposalType
import com.github.ajalt.clikt.core.CliktCommand
import net.postchain.client.core.PostchainClient
import net.postchain.common.types.RowId

interface ProposalDataProvider<T> {
    fun formatData(data: T, cliktCommand: CliktCommand): Any
    fun getData(client: PostchainClient, proposalId: RowId): T?
    fun getFormattedData(client: PostchainClient, proposalId: RowId, cliktCommand: CliktCommand): Any {
        val data = getData(client, proposalId) ?: return ""
        return formatData(data, cliktCommand)
    }
}

object ProposalDataProviderFactory {
    fun getDataProvider(proposalType: ProposalType, apiVersion: Long): ProposalDataProvider<out Any?>? {
        return if (apiVersion >= 33) {
            typeMap[proposalType]
        } else {
            legacyTypeMap[proposalType]
        }
    }

    //used for api version over and equal to 33
    private val typeMap: Map<ProposalType, ProposalDataProvider<out Any?>> = mapOf(
            ProposalType.bc to BlockchainProposalDataProvider(),
            ProposalType.configuration to ConfigurationProposalDataProvider(),
            ProposalType.configuration_at to ConfigurationProposalAtDataProvider(),
            ProposalType.force_configuration to ProposedForcedConfigurationDataProvider(),
            ProposalType.voter_set_update to VoterSetUpdateProposalDataProvider(),
            ProposalType.blockchain_action to BlockchainUnarchiveActionProposalDataProvider(),
            ProposalType.blockchain_move_start to BlockchainMoveProposalDataProvider(),
            ProposalType.blockchain_move_finish to BlockchainMoveFinishProposalDataProvider()
    )

    //used for api version under 33
    private val legacyTypeMap: Map<ProposalType, ProposalDataProvider<out Any?>> = mapOf(
            ProposalType.bc to BlockchainProposalDataProvider(),
            ProposalType.configuration to ConfigurationProposalDataProvider(),
            ProposalType.configuration_at to ConfigurationProposalAtDataProvider(),
            ProposalType.force_configuration to ProposedForcedConfigurationDataProvider(),
            ProposalType.voter_set_update to VoterSetUpdateProposalDataProvider(),
            ProposalType.blockchain_action to BlockchainActionProposalDataProvider()
    )
}