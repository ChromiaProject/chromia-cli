package com.chromia.cli.command.deployment.proposal

import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.BlockchainConfigurationData
import com.chromia.directory1.proposal_blockchain.GetBlockchainActionProposalResult
import com.chromia.directory1.proposal_blockchain.GetBlockchainProposalResult
import com.chromia.directory1.proposal_blockchain.GetBlockchainUnarchiveActionProposalResult
import com.chromia.directory1.proposal_blockchain.GetConfigurationProposalAtResult
import com.chromia.directory1.proposal_blockchain.GetConfigurationProposalResult
import com.chromia.directory1.proposal_blockchain.GetProposedForcedConfigurationResult
import com.chromia.directory1.proposal_blockchain.PendingBlockchainConfigurationAtData
import com.chromia.directory1.proposal_blockchain.PendingBlockchainConfigurationData
import com.chromia.directory1.proposal_blockchain.ProposedForcedConfigurationData
import com.chromia.directory1.proposal_blockchain_move.GetBlockchainMoveFinishProposalResult
import com.chromia.directory1.proposal_blockchain_move.GetBlockchainMoveProposalResult
import com.chromia.directory1.proposal_voter_set.GetVoterSetUpdateProposalResult
import net.postchain.common.types.RowId
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.mapper.GtvObjectMapper

fun finishedMoveProposal(blockchainRid: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetBlockchainMoveFinishProposalResult(
        blockchainRid = blockchainRid,
        blockchainName = "",
        cluster = "cluster1",
        container = "container1",
        finalHeight = 10
))

fun blockchainActionProposal(blockchainRid: WrappedByteArray, action: BlockchainAction) = GtvObjectMapper.toGtvDictionary(GetBlockchainActionProposalResult(
        blockchain = blockchainRid,
        blockchainName = "",
        action = action
))

fun blockchainUnarchiveActionProposal(blockchainRid: WrappedByteArray, action: BlockchainAction) = GtvObjectMapper.toGtvDictionary(GetBlockchainUnarchiveActionProposalResult(
        blockchain = blockchainRid,
        blockchainName = "",
        action = action,
        sourceContainer = "source",
        destinationContainer = "destination",
        finalHeight = 100
))

fun startedMoveProposal(blockchainRid: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetBlockchainMoveProposalResult(
        blockchainRid = blockchainRid,
        blockchainName = "",
        cluster = "cluster1",
        container = "container1"
))

fun bcProposal(data: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetBlockchainProposalResult(
        data = data,
        container = "container1"
))

fun configurationProposal(data: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetConfigurationProposalResult(
        currentConf = BlockchainConfigurationData(height = 1, blockchain = RowId(1), data = data),
        proposedConf = PendingBlockchainConfigurationData(proposal = RowId(22), blockchain = RowId(1), data = data)
))

fun configurationAtProposal(data: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetConfigurationProposalAtResult(
        currentConf = BlockchainConfigurationData(height = 1, blockchain = RowId(1), data = data),
        proposedConf = PendingBlockchainConfigurationAtData(proposal = RowId(22), blockchain = RowId(1), data = data, height = 10, force = false)
))

fun configurationForceProposal(data: WrappedByteArray) = GtvObjectMapper.toGtvDictionary(GetProposedForcedConfigurationResult(
        currentConf = BlockchainConfigurationData(height = 1, blockchain = RowId(1), data = data),
        forcedConf = ProposedForcedConfigurationData(proposal = RowId(22), blockchain = RowId(1), height = 10, configData = data, resumeChain = false),
        resumeChain = false
))

fun votersetUpdateProposal() = GtvObjectMapper.toGtvDictionary(GetVoterSetUpdateProposalResult(
        voterSet = "voterSet",
        threshold = 2,
        addMember = listOf(),
        governor = "voterset_gov",
        removeMember = listOf()
))