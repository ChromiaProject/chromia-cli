package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.testData
import java.nio.file.Path
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GtxQuery
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
                    url: "$apiUrl"
                    brid: x"0000000000000000000000000000000000000000000000000000000000000000"
                    container: testcontainer
                    chains:
                      hello: x"${testBrid.toHex()}"
            """.trimIndent())
            }
        }
        withModel(
                Directory1Model(BlockchainRid.ZERO_RID, mapOf(testBrid to listOf(apiUrl))),
                QueryDeploymentModel(testBrid),
        ) {
            TestProcess.Builder("query", "hello_world", "--network", "test", "--blockchain", "hello")
                    .setConfig(dir.resolve("chromia.yml").toFile())
                    .startCondition("Hello People!")
                    .start()
        }
    }
}
