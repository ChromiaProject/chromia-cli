package com.chromia.cli.command.deployment.proposal

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.cli.util.DeploymentTestDataCreator.pubkey
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.common.queries.GET_PROVIDER_DATA
import com.chromia.directory1.model.Provider
import com.chromia.directory1.model.ProviderTier
import com.chromia.directory1.proposal.GET_PROPOSAL
import com.chromia.directory1.proposal.GET_PROPOSAL_VOTING_RESULTS
import com.chromia.directory1.proposal.ProposalData
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.ProposalVotingResults
import com.chromia.directory1.proposal.voting.VotingResult
import com.chromia.directory1.proposal_blockchain.BlockchainAction
import com.chromia.directory1.proposal_blockchain.GetBlockchainActionProposalResult
import com.chromia.directory1.proposal_blockchain.GetBlockchainProposalResult
import com.chromia.directory1.proposal_blockchain.GetBlockchainUnarchiveActionProposalResult
import com.chromia.directory1.proposal_blockchain.GetConfigurationProposalAtResult
import com.chromia.directory1.proposal_blockchain.GetConfigurationProposalResult
import com.chromia.directory1.proposal_blockchain.GetProposedForcedConfigurationResult
import com.chromia.directory1.proposal_blockchain_move.GetBlockchainMoveFinishProposalResult
import com.chromia.directory1.proposal_blockchain_move.GetBlockchainMoveProposalResult
import com.chromia.directory1.proposal_voter_set.GetVoterSetUpdateProposalResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.common.types.RowId
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.Test

class PendingProposalModel(val model: Model, private val proposalResponse: Gtv, private val proposalType: ProposalType) : Model by model {
    constructor(blockchainRid: BlockchainRid, actionResponse: Gtv, proposalType: ProposalType) : this(TestModel(blockchainRid), actionResponse, proposalType)

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(gtv(RestApiInstance.apiUrl))
            GET_PROPOSAL -> GtvObjectMapper.toGtvDictionary(
                    ProposalData(
                            id = RowId(1),
                            timestamp = 9999999,
                            type = proposalType,
                            proposedBy = pubkey.hexStringToWrappedByteArray(),
                            description = "description",
                            state = ProposalState.PENDING,
                            applyAt = null,
                           	voterSetName = null,
                           	txRid = null,
                    ))

            GET_PROVIDER_DATA -> GtvObjectMapper.toGtvDictionary(Provider(
                    pubkey = pubkey.hexStringToWrappedByteArray(),
                    name = "",
                    url = "",
                    active = true,
                    system = true,
                    tier = ProviderTier.NODE_PROVIDER
            ))

            GET_PROPOSAL_VOTING_RESULTS -> GtvObjectMapper.toGtvDictionary(ProposalVotingResults(
                    positiveVotes = 1,
                    negativeVotes = 0,
                    maxVotes = 1,
                    threshold = 1,
                    votingResult = VotingResult.approved,
                    voterSetName = null,
            ))

            else -> proposalResponse
        }
    }
}

internal class ProposalInfoCommandTest {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var config: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    val model = DirectoryChainModel()

    private val gson: Gson = GsonBuilder().create()

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
        settings = parseModel(settingsFile)
        config = testDir.resolve("config").toFile()
    }

    @AfterEach
    fun tearDown() {
        logger.clearOutput()
    }

    @Test
    fun votersetUpdateProposalTest() {
        val type = ProposalType.voter_set_update
        withModel(PendingProposalModel(model.blockchainRid, votersetUpdateProposal(), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetVoterSetUpdateProposalResult::class.java, type)
    }

    @Test
    fun configurationForceProposalTest() {
        val type = ProposalType.force_configuration
        withModel(PendingProposalModel(model.blockchainRid, configurationForceProposal(WrappedByteArray(1)), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetProposedForcedConfigurationResult::class.java, type)
    }

    @Test
    fun configurationAtProposalTest() {
        val type = ProposalType.configuration
        withModel(PendingProposalModel(model.blockchainRid, configurationAtProposal(WrappedByteArray(1)), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetConfigurationProposalAtResult::class.java, type)
    }

    @Test
    fun configurationProposalTest() {
        val type = ProposalType.configuration
        withModel(PendingProposalModel(model.blockchainRid, configurationProposal(WrappedByteArray(1)), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetConfigurationProposalResult::class.java, type)
    }

    @Test
    fun bcProposalTest() {
        val type = ProposalType.bc
        withModel(PendingProposalModel(model.blockchainRid, bcProposal(WrappedByteArray(1)), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetBlockchainProposalResult::class.java, type)
    }

    @Test
    fun pendingBlockchainMoveStartedProposalTest() {
        val type = ProposalType.blockchain_move_start
        withModel(PendingProposalModel(model.blockchainRid, startedMoveProposal(model.blockchainRid.wData), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetBlockchainMoveProposalResult::class.java, type)
    }

    @Test
    fun pendingBlockchainMoveFinishedProposalTest() {
        val type = ProposalType.blockchain_move_finish
        withModel(PendingProposalModel(model.blockchainRid, finishedMoveProposal(model.blockchainRid.wData), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        assertPendingProposal(GetBlockchainMoveFinishProposalResult::class.java, type)
    }

    @Test
    fun pendingBlockchainActionPauseProposalTest() {
        val type = ProposalType.blockchain_action
        withModel(PendingProposalModel(model.blockchainRid, blockchainUnarchiveActionProposal(model.blockchainRid.wData, BlockchainAction.pause), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1"))
        }
        val jsonObject = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        val proposal = jsonObject["proposal"].asJsonObject
        val description = jsonObject["proposalDescription"].asJsonObject

        assertThat(proposal["state"].asString).isEqualTo(ProposalState.PENDING.name)
        assertThat(proposal["timestamp"].asInt).isEqualTo(9999999)
        assertThat(proposal["type"].asString).isEqualTo(type.name)

        val blockchainAction = description["first"].asJsonObject
        for (field in GetBlockchainActionProposalResult::class.java.declaredFields) {
            assertThat(blockchainAction[field.name]).isNotNull()
        }

        assertThat(description["second"].isJsonNull)
    }

    @Test
    fun pendingBlockchainActionUnarchiveProposalTest() {
        val type = ProposalType.blockchain_action
        withModel(PendingProposalModel(model.blockchainRid, blockchainUnarchiveActionProposal(model.blockchainRid.wData, BlockchainAction.unarchive), type)) {
            ProposalInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--id", "1", "--output-format", "JSON"))
        }
        val jsonObject = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        val proposal = jsonObject["proposal"].asJsonObject
        val description = jsonObject["proposalDescription"].asJsonObject

        assertThat(proposal["state"].asString).isEqualTo(ProposalState.PENDING.name)
        assertThat(proposal["timestamp"].asInt).isEqualTo(9999999)
        assertThat(proposal["type"].asString).isEqualTo(type.name)

        val blockchainAction = description["first"].asJsonObject
        for (field in GetBlockchainActionProposalResult::class.java.declaredFields) {
            assertThat(blockchainAction[field.name]).isNotNull()
        }

        val unarchiveProposal = description["second"].asJsonObject
        for (field in GetBlockchainUnarchiveActionProposalResult::class.java.declaredFields) {
            assertThat(unarchiveProposal[field.name]).isNotNull()
        }
    }

    private fun <T> assertPendingProposal(targetDescription: Class<T>, proposalType: ProposalType) {
        val jsonObject = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        val proposal = jsonObject["proposal"].asJsonObject
        val description = jsonObject["proposalDescription"].asJsonObject

        assertThat(proposal["state"].asString).isEqualTo(ProposalState.PENDING.name)
        assertThat(proposal["timestamp"].asInt).isEqualTo(9999999)
        assertThat(proposal["type"].asString).isEqualTo(proposalType.name)

        for (field in targetDescription.declaredFields) {
            assertThat(description[field.name]).isNotNull()
        }
    }
}