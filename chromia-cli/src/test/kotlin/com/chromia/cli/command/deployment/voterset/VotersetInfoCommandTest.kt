package com.chromia.cli.command.deployment.voterset

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.TestModel
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.common.queries.GET_VOTER_SET_INFO
import com.chromia.directory1.common.queries.GetVoterSetInfoResult
import com.github.ajalt.clikt.core.context
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

class VotersetInfoModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(RestApiInstance.apiUrl)))
            GET_VOTER_SET_INFO -> GtvObjectMapper.toGtvDictionary(
                    GetVoterSetInfoResult(
                            name = "vs1",
                            threshold = 0,
                            governor = "vs1",
                            members = listOf(
                                    "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765".hexStringToWrappedByteArray(),
                                    "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05".hexStringToWrappedByteArray())
                    ))

            else -> throw IllegalStateException("${query.name}  is not supported")
        }
    }
}

internal class VotersetInfoCommandTest {
    val model = DirectoryChainModel()
    private val gson: Gson = GsonBuilder().create()

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    private val testTerminal = Terminal(logger)

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
    fun votersetInfoDataTest() {
        RestApiInstance.withModel(VotersetInfoModel(model.blockchainRid)) {
            VotersetInfoCommand().context { terminal = testTerminal }.parse(listOf("--settings", settingsFile.absolutePath, "--network", "test", "--name", "vs1"))
        }
        val voterset = gson.fromJson(logger.output(), JsonObject::class.java).asJsonObject
        assertThat(voterset["Voter set"].asString).isEqualTo("vs1")
        assertThat(voterset["Governed by"].asString).isEqualTo("vs1")
        assertThat(voterset["Threshold"].asString).isEqualTo("super majority (>66.66%)")
        assertThat(voterset["members"].asJsonArray.size()).isEqualTo(2)
    }
}