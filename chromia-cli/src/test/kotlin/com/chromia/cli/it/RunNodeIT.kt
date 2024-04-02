package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.cli.util.testData
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.time.Duration

class RunNodeIT {
    @Test
    fun startNode(@TempDir dir: Path) {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start { process ->
                    TestProcess.Builder("query", "hello").startCondition("Hi!").start()
                    TestProcess.Builder("tx", "call_op", "1").startCondition("was posted WAITING: OK").start()

                    TestProcess.Builder("query", "new_query").startCondition("Unknown query: new_query").exitCode(1).start()
                    with(File(dir.toFile(), "src/main.rell")) {
                        writeText("""
                            module;
                            query hello() = "Hi!";
                            query new_query() = 1;
                        """.trimIndent())
                    }
                    TestProcess.Builder("node", "update", "-n", "5")
                            .verbose()
                            .startCondition("Configuration added at height")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .start()
                    process.waitUntil("Blockchain has been started", Duration.ofSeconds(30))

                    TestProcess.Builder("query", "new_query").startCondition("1").start()
                }
        // Restarting the node should be fine
        TestProcess.Builder("node", "start")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setWorkingDir(dir.toFile())
                .start {
                    // Same blockchain was started
                    TestProcess.Builder("query", "new_query").startCondition("1").start()

                    // Fail to update using configuration that already exists
                    testData(dir)
                    TestProcess.Builder("node", "update")
                            .setConfig(dir.resolve("chromia.yml").toFile())
                            .startCondition("Blockchain configuration already exists in database, cannot update")
                            .start()
                }

    }

    @Test
    fun icmfMessageIsReceived(@TempDir dir: Path) {

        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      sender:
                        module: sender
                        config:
                          blockstrategy:
                            maxblocktime: 1000
                          gtx:
                            modules:
                              - "net.postchain.d1.icmf.IcmfSenderGTXModule"
                      receiver:
                        module: receiver
                        config:
                          icmf:
                            receiver:
                              local:
                                - bc-rid: x""
                                  topic: "L_msg"
                          gtx:
                            modules:
                              - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                          sync_ext:
                            - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"
                """.trimIndent())
            }
            addFile("sender.rell", """
                module;
                operation send_message(text) {
                    op_context.emit_event("icmf_message", (topic = "L_msg", body = text.to_gtv()).to_gtv_pretty());
                }
            """.trimIndent())
            addFile("receiver.rell", """
                module;
                entity msg {
                  topic: text;
                  msg: text;
                }
                operation __icmf_message(sender: byte_array, topic: text, body: gtv) {
                    create msg(topic = topic, msg = text.from_gtv(body));
                }
                query get_messages() = msg @*{}($.to_struct());
                operation dummy() {}
            """.trimIndent())
        }
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setWorkingDir(dir.toFile())
                .start {
                    // Send message
                    TestProcess.Builder("tx", "--cid", "0", "send_message", "Hello!", "--await").startCondition("was posted CONFIRMED").start()
                    // Make sure a block gets built by making a dummy operation (We could also just wait maxBlockTime)
                    TestProcess.Builder("tx", "--cid", "1", "dummy", "--await").startCondition("was posted CONFIRMED").start()
                    // Verify that message was received
                    TestProcess.Builder("query", "--cid", "1", "get_messages").startCondition("""[["msg": "Hello!", "topic": "L_msg"]]""").start()
                }
    }

    @Test
    fun startNodeWithNonWhitelistedGtxModuleFails(@TempDir dir: Path) {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        config:
                          gtx:
                            modules:
                              - "net.postchain.eif.transaction.TransactionSubmitterGTXModule"
                """.trimIndent())
            }
        }

        TestProcess.Builder("node", "start", "--wipe")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("An error occurred. Could not find Gtx module: net.postchain.eif.transaction.TransactionSubmitterGTXModule")
                .start()
    }
}
