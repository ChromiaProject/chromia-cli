package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.integration.RellApiRunIntegrationTests
import com.chromia.cli.util.testData
import java.nio.file.Path
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.SingleEndpointPool
import net.postchain.common.BlockchainRid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RellApiRunIntegrationTestsTest {
    @Test
    fun itTest(@TempDir dir: Path) {
        testData(dir) {
            addFile("test/it.rell", """
                @test module;
                
                import main.*;
                
                function it_do_query() {
                    print(hello());
                    rell.test.tx().op(call_op(1)).run();
                }
            """.trimIndent())
        }

        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .verbose()
                .startCondition("Blockchain has been started")
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    val conf = PostchainClientConfig(BlockchainRid.buildFromHex("4139CD191C40A78CC2708CF16C7F81A2F639549A795960D1EEBABCA9DA093A8F"), SingleEndpointPool("http://localhost:7740"))
                    val client = PostchainClientImpl(conf)
                    RellApiRunIntegrationTests.runTests(RellApiRunIntegrationTests.Config.DEFAULT, dir.resolve("src").toFile(), listOf("test"), client)
                }
    }
}