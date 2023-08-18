package com.chromia.cli.it

import com.chromia.build.tools.TestModel
import com.chromia.build.tools.TestProcess
import com.chromia.cli.util.testData
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.api.rest.controller.RestApi
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class QueryDeploymentModel(val model: Model) : Model by model {
    constructor(blockchainRid: BlockchainRid) : this(TestModel(blockchainRid))

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
        testData(dir) {
            config {
                deployments(
                    """
                deployments:
                  test:
                    url: "http://localhost:7741"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
                    chains:
                      hello: x"${testBrid.toHex()}"
            """.trimIndent())
            }
        }
        RestApi(7741, "").use {
            it.attachModel(BlockchainRid.ZERO_RID, Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf("http://localhost:7741"))))
            it.attachModel(testBrid, QueryDeploymentModel(testBrid))

            TestProcess.Builder("query", "hello_world", "--network", "test", "--blockchain", "hello")
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .start { process ->
                        assertThat(process.readLines()).anyMatch { it.contains("Hello People!") }
                    }
        }
    }
}
