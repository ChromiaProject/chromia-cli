package com.chromia.cli.command.deployment

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_CLUSTER
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import net.postchain.api.rest.BlockHeight
import net.postchain.api.rest.controller.Model
import net.postchain.client.exception.ClientError
import net.postchain.common.BlockchainRid
import net.postchain.ebft.rest.contract.StateNodeStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

private fun testClient(): (Request) -> Response = {
    //TODO Migrate if the postchain client gains an offical handler for it.
    // Used to resolve with model to be used for each request. As there are no offical client calls for iid_ both need to be mocked with a custom httpHandler.
    when {
        it.uri.path.contains("brid/iid_0") -> Response(Status.OK).body("0000000000000000000000000000000000000000000000000000000000000000")
        it.uri.path.contains("06") -> Response(Status.OK).body("{\"state\":\"HaveBlock\",\"height\":119329,\"serial\":159158134941,\"round\":2,\"blockRid\":\"328B9498981B459226B2E2B97B5E0AB4C0F4580D98E7C2F14DBA384FFFD90F80\",\"revolting\":true}")
        else -> Response(Status.NOT_FOUND).body("{\"error\":\"Can't find blockchain\"}")
    }
}

class DeployedChainModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }

    override fun getCurrentBlockHeight(): BlockHeight {
        return when (blockchainRid) {
            BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002") -> BlockHeight(569889)
            BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005") -> throw ClientError("Context", Status.INTERNAL_SERVER_ERROR, "Module initialization error", null)
            BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> BlockHeight(570320)
            else -> throw ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", null)
        }
    }

    override fun nodeStatusQuery(): StateNodeStatus {
        return when (blockchainRid) {
            BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> StateNodeStatus(pubKey = "", type = "", state = "HaveBlock", height = 119329, serial = 159158134941, round = 2, blockRid = "328B9498981B459226B2E2B97B5E0AB4C0F4580D98E7C2F14DBA384FFFD90F80", revolting = true)
            else -> throw ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", null)
        }
    }
}

class Directory1Model(val model: Model, val responseChain: BlockchainRid) : Model by model {
    constructor(blockchainRid: BlockchainRid, responseChain: BlockchainRid) : this(TestModel(blockchainRid), responseChain)

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "api_version" -> gtv(33)
            CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(RestApiInstance.apiUrl)))
            CM_GET_BLOCKCHAIN_CLUSTER -> {
                return when (responseChain) {
                    BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002") -> gtv("my_cluster")
                    BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000003") -> gtv("my_cluster")
                    BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005") -> gtv("my_cluster")
                    BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006") -> gtv("my_cluster")
                    BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000004") -> throw ClientError("Context", Status.NOT_FOUND, "Can't find blockchain", null)
                    else -> throw IllegalStateException("Unsupported blockchain RID: $responseChain for cluster")
                }
            }

            else -> throw IllegalStateException("${query.name}  is not supported")
        }
    }
}


class DeployInfoCommandTest {
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
    fun failedVerification(@TempDir dir: Path) {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            ok:        x"0000000000000000000000000000000000000000000000000000000000000002"
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "ok", "--network", "test"))
            assertBlockchainTableContainsRow("ok", "0000000000000000000000000000000000000000000000000000000000000002", "my_cluster")
            assertNodeTableContainsRow("http://localhost:7745", "569889", "OK")
            logger.clearOutput()
            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000002", "--api-url", "http://localhost:7745"))
            assertBlockchainTableContainsRow("0000000000000000000000000000000000000000000000000000000000000002", "0000000000000000000000000000000000000000000000000000000000000002", "my_cluster")
            logger.clearOutput()
        }
    }

    @Test
    fun CanNotFindBlockchain(@TempDir dir: Path) {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000003")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            not_found:    x"0000000000000000000000000000000000000000000000000000000000000003"
 
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "not_found", "--network", "test"))

//        assertNodeTableContainsRow("http://localhost:7745", "-1", "Context: 404 Not Found  Can't find blockchain from http://localhost:7745")
            //TODO Revert to old message when new verison of tools is implemented for hight finder
            assertNodeTableContainsRow("http://localhost:7745", "-1", "currentBlockHeight: 400 Bad Request  Context: 404 Not Found  Can't find blockchain  from http://localhost:7745")
            logger.clearOutput()
            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000003", "--api-url", "http://localhost:7745"))
            assertNodeTableContainsRow("http://localhost:7745", "-1", "currentBlockHeight: 400 Bad Request  Context: 404 Not Found  Can't find blockchain  from http://localhost:7745")
            logger.clearOutput()
        }
    }

    @Test
    fun notDeployed(@TempDir dir: Path) {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000004")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            not_deployed: x"0000000000000000000000000000000000000000000000000000000000000004"
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "not_deployed", "--network", "test"))
            assertThat(logger.stdout()).contains("Cluster not found for blockchain rid 00:004")
            logger.clearOutput()
            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000004", "--api-url", "http://localhost:7745"))
            assertThat(logger.stdout()).contains("Cluster not found for blockchain rid 00:004")
            logger.clearOutput()
        }
    }

    @Test
    fun hasErrors(@TempDir dir: Path) {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000005")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            has_errors:   x"0000000000000000000000000000000000000000000000000000000000000005"
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "has_errors", "--network", "test"))
            //TODO Revert to old message when new verison of tools is implemented for hight finder
            assertNodeTableContainsRow("http://localhost:7745", "-1", "currentBlockHeight: 400 Bad Request  Context: 500 Internal Server Error  Module initialization error  from http://localhost:7745")
//        assertNodeTableContainsRow("http://localhost:7745", "-1", "Context: 500 Internal Server Error  Module initialization error from http://localhost:7745")
            logger.clearOutput()
            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000005", "--api-url", "http://localhost:7745"))
            assertNodeTableContainsRow("http://localhost:7745", "-1", "currentBlockHeight: 400 Bad Request  Context: 500 Internal Server Error  Module initialization error  from http://localhost:7745")
            logger.clearOutput()
        }
    }

    @Test
    fun haveBlock(@TempDir dir: Path) {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000006")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            have_block:   x"0000000000000000000000000000000000000000000000000000000000000006"
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "have_block", "--network", "test"))
            assertNodeTableContainsRow("http://localhost:7745", "570320", "OK")
            logger.clearOutput()
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "have_block", "--network", "test", "--verbose"))
            assertVerboseNodeTableContainsRow("http://localhost:7745", "119329", "HaveBlock", "2", "true", "OK")
            logger.clearOutput()

            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--api-url", "http://localhost:7745"))
            assertNodeTableContainsRow("http://localhost:7745", "570320", "OK")
            logger.clearOutput()
            DeployInfoCommand { testClient() }.context { terminal = testTerminal }.parse(listOf("-brid", "0000000000000000000000000000000000000000000000000000000000000006", "--api-url", "http://localhost:7745", "--verbose"))
            assertVerboseNodeTableContainsRow("http://localhost:7745", "119329", "HaveBlock", "2", "true", "OK")
            logger.clearOutput()
        }
    }

    @Test
    fun tableOutput() {
        val targetChain = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002")
        withModel(
                Directory1Model(model.blockchainRid, targetChain),
                DeployedChainModel(targetChain)) {
            testData(testDir) {
                config {
                    blockchains("""
                    blockchains:
                      ok:
                        module: main
                """.trimIndent())
                    deployments("""
                    deployments:
                      test:
                        url: "http://localhost:7745"
                        brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                        chains:
                            ok:        x"0000000000000000000000000000000000000000000000000000000000000002"
                """.trimIndent())
                }
            }
            DeployInfoCommand().context { terminal = testTerminal }.parse(listOf("-s", settingsFile.absolutePath, "--blockchain", "ok", "--network", "test", "--output-format", "table"))
            assertThat(logger.stdout()).contains("╭")
        }
    }

    private fun assertBlockchainTableContainsRow(blockchain: String, blockchainRid: String, cluster: String) {
        val info = gson.fromJson(logger.stdout(), JsonObject::class.java)["blockchain"].asJsonObject
        assertThat(info["Blockchain"].asString).isEqualTo(blockchain)
        assertThat(info["Blockchain_Rid"].asString).isEqualTo(blockchainRid)
        assertThat(info["Cluster"].asString).isEqualTo(cluster)
    }

    private fun assertNodeTableContainsRow(nodeUrl: String, height: String, status: String) {
        val nodes = gson.fromJson(logger.stdout(), JsonObject::class.java)["nodes"].asJsonArray
        val response = nodes[0].asJsonObject
        assertThat(response["Node_url"].asString).isEqualTo(nodeUrl)
        assertThat(response["Height"].asString).isEqualTo(height)
        assertThat(response["Status"].asString).isEqualTo(status)
    }

    private fun assertVerboseNodeTableContainsRow(nodeUrl: String, height: String, state: String, round: String, revolting: String, status: String) {
        val nodes = gson.fromJson(logger.stdout(), JsonObject::class.java)["nodes"].asJsonArray
        val response = nodes[0].asJsonObject
        assertThat(response["Node_url"].asString).isEqualTo(nodeUrl)
        assertThat(response["Height"].asString).isEqualTo(height)
        assertThat(response["State"].asString).isEqualTo(state)
        assertThat(response["Round"].asString).isEqualTo(round)
        assertThat(response["Revolting"].asString).isEqualTo(revolting)
        assertThat(response["Status"].asString).isEqualTo(status)
    }
}
