package com.chromia.cli.it

import java.io.File
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.api.rest.model.ApiStatus
import net.postchain.api.rest.model.TxRid
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.tx.TransactionStatus
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class Directory1Model(val model: Model, val chains: Map<BlockchainRid, List<String>>) : Model by model {
    constructor(blockchainRid: BlockchainRid, chains: Map<BlockchainRid, List<String>>) : this(TestModelImpl(blockchainRid), chains)
    override fun query(query: GtxQuery): Gtv {
        require(query.name == "cm_get_blockchain_api_urls")
        val brid = BlockchainRid(query.args.asDict()["blockchain_rid"]!!.asByteArray())
        return gtv(chains[brid]!!.map { gtv(it) })
    }
}

class QueryDeploymentModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModelImpl(blockchainRid))

    override fun getStatus(txRID: TxRid) = ApiStatus(TransactionStatus.CONFIRMED)
    override fun postTransaction(tx: ByteArray) {}

    override fun query(query: GtxQuery): Gtv {
        return when (query.name) {
            "hello_world" -> gtv("Hello People!")
            else -> throw IllegalArgumentException("Unknown result for query ${query.name}")
        }
    }
}

class QueryIT {

    @Test
    fun queryTowardsDeployment(@TempDir dir: Path) {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
        TestDataCreator.basicApp(dir)
        with(File(dir.toFile(), "config.yml")) {
            appendText("\n")
            appendText("""
                deployments:
                  test:
                    url: "http://localhost:7741"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
                    chains:
                      hello: x"${testBrid.toHex()}"
            """.trimIndent())
        }
        RestApi(7741, "").use {
            it.attachModel(BlockchainRid.ZERO_RID, Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf("http://localhost:7741"))))
            it.attachModel(testBrid, QueryDeploymentModel(testBrid))

            ChrProcess.Builder("query", "hello_world", "--network", "test", "--blockchain", "hello")
                    .setConfig(dir.resolve("config.yml").toFile())
                    .start { process ->
                        assertThat(process.readLines()).anyMatch { it.contains("Hello People!") }
                    }
        }
    }
}
