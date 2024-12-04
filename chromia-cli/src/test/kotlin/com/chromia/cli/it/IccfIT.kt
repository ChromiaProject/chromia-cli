package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.build.tools.testData
import java.nio.file.Path
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.SingleEndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.toHex
import net.postchain.gtv.GtvFactory.gtv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class IccfIT {

    @Test
    fun iccfIT(@TempDir dir: Path) {
        // If you change the code or update the rell version, verify the new brids to be used in the test
        // adding .verbose() before start will print the output of the command including new brids
        testData(dir) {
            config {
                blockchains(
                        """
                           blockchains:
                                source_chain:
                                    module: main
                                confirming:
                                    module: confirmation
                                    config:
                                        gtx:
                                            modules:
                                                - net.postchain.d1.iccf.IccfGTXModule
                        """.trimIndent()
                )
            }
            content("""
               module;
               operation op_to_confirm(msg: text) {}
               query get_latest_tx() = transaction @ {}.tx_rid;
            """.trimIndent())
            addSourceFile("confirmation.rell", """
                module;
                operation confirmation(tx: gtx_transaction) {
                require(op_context.get_all_operations()[0].name == "iccf_proof");
                require(text.from_gtv(tx.body.operations[0].args[0]) == "Secret message");
                }
            """.trimIndent())
        }
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    TestProcess.Builder("tx", "--cid", "0", "op_to_confirm", "Secret message", "--await").verbose().startCondition("was posted CONFIRMED").start()
                    val config = PostchainClientConfig(BlockchainRid.ZERO_RID, endpointPool = SingleEndpointPool("http://localhost:7740"), queryByChainId = 0)
                    val client = PostchainClientImpl(config)
                    val txRid = client.query("get_latest_tx", gtv(mapOf())).asByteArray().toHex()
                    TestProcess.Builder("tx", "--cid", "1", "--iccf-source", "7B8AA87E2A87860E49B0463D865985940BECA97087EDC1629296BA35EC2642DA", "--iccf-tx", txRid, "confirmation", "--await").startCondition("was posted CONFIRMED").start()
                }
    }
}