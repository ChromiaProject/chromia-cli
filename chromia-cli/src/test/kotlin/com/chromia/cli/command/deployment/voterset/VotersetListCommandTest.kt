package com.chromia.cli.command.deployment.voterset

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.TestModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.common.queries.ContainerData
import com.chromia.directory1.common.queries.GET_CONTAINER_DATA
import com.chromia.directory1.common.queries.GET_VOTER_SETS
import com.chromia.directory1.common.queries.GET_VOTER_SET_INFO
import com.chromia.directory1.common.queries.GetVoterSetInfoResult
import com.chromia.directory1.common.queries.GetVoterSetsResult
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
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

class VotersetListModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun queryWithHeight(query: GtxQuery): Pair<Gtv, Long> = query(query) to 0

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            GET_CONTAINER_DATA -> GtvObjectMapper.toGtvDictionary(ContainerData(
                    name = "c1",
                    cluster = "cluster1",
                    deployer = "vs1",
                    proposedByPubkey = "00".hexStringToWrappedByteArray(),
                    proposedByName = "name",
                    system = false,
                    state = null,
                    subnodeImage = null,
                    jarExtensions = null
            ))

            GET_VOTER_SET_INFO -> GtvObjectMapper.toGtvDictionary(GetVoterSetInfoResult(
                    name = "vs1",
                    threshold = 0,
                    governor = "vs1",
                    members = listOf(
                            "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765".hexStringToWrappedByteArray(),
                            "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05".hexStringToWrappedByteArray())
            ))

            CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(RestApiInstance.apiUrl)))
            GET_VOTER_SETS -> gtv(
                    listOf(
                            GtvObjectMapper.toGtvDictionary(GetVoterSetsResult(
                                    name = "vs1",
                                    threshold = 0,
                                    gorvernor = "vs1",
                            )),
                            GtvObjectMapper.toGtvDictionary(GetVoterSetsResult(
                                    name = "vs2",
                                    threshold = -1,
                                    gorvernor = "vs2",
                            ))))

            else -> throw IllegalStateException("${query.name}  is not supported")
        }
    }
}

internal class VotersetListCommandTest {
    val model = DirectoryChainModel()
    private val gson: Gson = GsonBuilder().create()

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(terminalInterface = logger)

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @AfterEach
    fun tearDown() {
        logger.clearOutput()
    }

    @Test
    fun votersetListDataTest() {
        RestApiInstance.withModel(VotersetListModel(model.blockchainRid)) {
            VotersetListCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test"))
        }
        val votersets = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        assertThat(votersets.size()).isEqualTo(2)
        assertThat(votersets["vs1"].asJsonObject["name"].asString).isEqualTo("vs1")
        assertThat(votersets["vs2"].asJsonObject["name"].asString).isEqualTo("vs2")

        assertThat(votersets["vs1"].asJsonObject["governor"].asString).isEqualTo("vs1")
        assertThat(votersets["vs2"].asJsonObject["governor"].asString).isEqualTo("vs2")

        assertThat(votersets["vs1"].asJsonObject["majorityLevel"].asString).isEqualTo("super majority (>66.66%)")
        assertThat(votersets["vs2"].asJsonObject["majorityLevel"].asString).isEqualTo("majority (>50%)")
    }


    @Test
    fun votersetListDataForContainerTest() {
        RestApiInstance.withModel(VotersetListModel(model.blockchainRid)) {
            VotersetListCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--container", "c1"))
        }
        val votersets = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        assertThat(votersets.size()).isEqualTo(1)
        assertThat(votersets["vs1"].asJsonObject["name"].asString).isEqualTo("vs1")
        assertThat(votersets["vs1"].asJsonObject["governor"].asString).isEqualTo("vs1")
        assertThat(votersets["vs1"].asJsonObject["majorityLevel"].asString).isEqualTo("super majority (>66.66%)")
    }
}