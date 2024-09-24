package com.chromia.cli.command.deployment.proposal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.model.parseModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.proposal.GET_PROPOSALS_RANGE
import com.chromia.directory1.proposal.GET_RELEVANT_PROPOSALS
import com.chromia.directory1.proposal.GetProposalsRangeResult
import com.chromia.directory1.proposal.ProposalState
import com.chromia.directory1.proposal.ProposalType
import com.chromia.directory1.proposal.voting.GET_PROVIDER_VOTES
import com.chromia.directory1.proposal.voting.GetProviderVotesResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.tx.TransactionStatus
import net.postchain.common.types.RowId
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.test.Test

class ProposalListModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    private val proposalOne = GtvObjectMapper.toGtvDictionary(GetProposalsRangeResult(rowid = RowId(1), proposalType = ProposalType.provider_batch, ProposalState.PENDING))
    private val proposalTwo = GtvObjectMapper.toGtvDictionary(GetProposalsRangeResult(rowid = RowId(2), proposalType = ProposalType.blockchain_action, ProposalState.PENDING))
    private val proposalThree = GtvObjectMapper.toGtvDictionary(GetProposalsRangeResult(rowid = RowId(3), proposalType = ProposalType.configuration, ProposalState.PENDING))

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(gtv(RestApiInstance.apiUrl))
            GET_PROPOSALS_RANGE -> gtv(listOf(
                    proposalOne,
                    proposalTwo,
                    proposalThree
            ))

            GET_RELEVANT_PROPOSALS -> gtv(listOf(
                    proposalOne,
                    proposalThree
            ))

            GET_PROVIDER_VOTES -> gtv(listOf(
                    GtvObjectMapper.toGtvDictionary(GetProviderVotesResult(proposal = RowId(1), vote = true)),
                    GtvObjectMapper.toGtvDictionary(GetProviderVotesResult(proposal = RowId(3), vote = false))
            ))

            else -> throw IllegalStateException("${query.name}  is not supported")
        }
    }
}

internal class ProposalListCommandTest {
    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private lateinit var settings: ChromiaModel
    private lateinit var config: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(logger)

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
    fun canNotFindIdentityToFilterProposalTest() {
        assertThrows<CanNotFindPubkeyException> {
            withModel(ProposalListModel(model.blockchainRid)) {
                ProposalListCommand().parse(listOf("--settings", settingsFile.absolutePath, "--network", "test"))
            }
        }
    }


    @Test
    fun getRelevantProposalsTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            withModel(ProposalListModel(model.blockchainRid)) {
                ProposalListCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test"))
            }
        }

        val jsonObject = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        val proposalOne = jsonObject["1"].asJsonObject
        assertThat(proposalOne["type"].asString).isEqualTo(ProposalType.provider_batch.name)
        assertThat(proposalOne["id"].asInt).isEqualTo(1)
        assertThat(proposalOne["state"].asString).isEqualTo(ProposalState.PENDING.name)
        assertThat(proposalOne["status"].asString).isEqualTo("Accept")

        val proposalThree = jsonObject["3"].asJsonObject
        assertThat(proposalThree["type"].asString).isEqualTo(ProposalType.configuration.name)
        assertThat(proposalThree["id"].asInt).isEqualTo(3)
        assertThat(proposalThree["state"].asString).isEqualTo(ProposalState.PENDING.name)
        assertThat(proposalThree["status"].asString).isEqualTo("Reject")
    }

    @Test
    fun getAllProposalsTest(@TempDir dir: Path) {
        testData(dir) {
            keyStore()
        }
        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            withModel(ProposalListModel(model.blockchainRid)) {
                ProposalListCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--all"))
            }
        }

        val jsonObject = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        assertThat(jsonObject.size()).isEqualTo(3)
        assertThat(jsonObject["1"].asJsonObject["id"].asInt).isEqualTo(1)
        assertThat(jsonObject["2"].asJsonObject["id"].asInt).isEqualTo(2)
        assertThat(jsonObject["2"].asJsonObject["status"].asString).isEqualTo("No vote registered")
        assertThat(jsonObject["3"].asJsonObject["id"].asInt).isEqualTo(3)
    }
}