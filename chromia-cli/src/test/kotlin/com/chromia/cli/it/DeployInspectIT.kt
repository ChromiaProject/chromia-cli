package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import com.chromia.cli.command.node.INITILIZED_LOG
import java.nio.file.Path
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientProviderImpl
import net.postchain.client.request.EndpointPool
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class DeployInspectIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    val brid = getBrid()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", "$brid").startCondition("Module: main").start()
                    TestProcess.Builder("deployment", "inspect", "--url", "http://localhost:7740", "-brid", "$brid", "-l").startCondition("main").start()
                }
    }

    private fun getBrid(): BlockchainRid {
        val clientConfig = PostchainClientConfig(BlockchainRid.ZERO_RID, endpointPool = EndpointPool.singleUrl("http://localhost:7740"))
        val brid = PostchainClientProviderImpl().createClient(clientConfig).getBlockchainRID(0)
        return brid
    }
}
