package com.chromia.cli.it

import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.testData
import com.chromia.cli.command.node.INITILIZED_LOG
import java.io.File
import java.nio.file.Path
import java.time.Duration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class RunNodeIT {

    @TempDir
    lateinit var dir: Path

    @Test
    fun startNode() {
        testData(dir)
        TestProcess.Builder("node", "start", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start { process ->
                    TestProcess.Builder("query", "hello").startCondition("Hi!").start()
                    TestProcess.Builder("tx", "call_op", "1", "--no-await").startCondition("was posted WAITING: OK").start()

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
    fun startNodeWithLibraryChain() {
        testData(dir) {
            addSourceFile("lib/my_lib/module.rell", """module; query hello() = "Hello!";""")
            addSourceFile("tests/dapp.rell", """module; import lib.my_lib;""")
            config {
                blockchains("""
                    blockchains:
                      my_lib:
                        module: lib.my_lib
                        type: library
                      my_test_chain:
                        module: tests.dapp
                """.trimIndent())
            }
        }
        TestProcess.Builder("node", "start")
                .verbose()
                .startCondition(INITILIZED_LOG)
                .awaitCompletion(false)
                .setWorkingDir(dir.toFile())
                .start {
                    TestProcess.Builder("query", "hello").startCondition("Hello!").start()
                }
    }

    @Test
    fun icmfMessageIsReceived() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      sender:
                        module: sender
                        config:
                          blockstrategy:
                            mininterblockinterval: 25
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
                                - brid: null
                                  topic: "L_msg"
                          gtx:
                            modules:
                              - "net.postchain.d1.icmf.IcmfReceiverGTXModule"
                          sync_ext:
                            - "net.postchain.d1.icmf.IcmfReceiverSynchronizationInfrastructureExtension"
                """.trimIndent())
            }
            addSourceFile("sender.rell", """
                module;
                operation send_message(text) {
                    op_context.emit_event("icmf_message", (topic = "L_msg", body = text.to_gtv()).to_gtv_pretty());
                }
            """.trimIndent())
            addSourceFile("receiver.rell", """
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
                    TestProcess.Builder("query", "--cid", "1", "get_messages").wholeOutput("""
                        |[
                        |  [
                        |    "msg": "Hello!",
                        |    "topic": "L_msg"
                        |  ]
                        |]""".trimMargin()).start()
                }
    }

    @Test
    fun startNodeWithNonWhitelistedGtxModuleFails() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: main
                        config:
                          gtx:
                            modules:
                              - "net.postchain.UnknownGTXModule"
                """.trimIndent())
            }
        }

        TestProcess.Builder("node", "start", "--wipe")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition("GTX module class not found: net.postchain.UnknownGTXModule")
                .start()
    }

    @Test
    fun startNodeWithGtxStrictConfigurationTrue() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: strictmode
                    compile:
                      strictGtvConversion: true
                      rellVersion: 0.13.9
                """.trimIndent())
            }
            addSourceFile("strictmode.rell", """
                module;
                operation strict_gtv(arg: integer) {}
            """.trimIndent())
        }

        TestProcess.Builder("node", "start", "--wipe")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .start {
                    TestProcess.Builder("tx", "--cid", "0", "strict_gtv", "25L", "--await").exitCode(1).start { process ->
                        process.waitUntil("Decoding type 'integer': expected INTEGER, actual BIGINTEGER", Duration.ofSeconds(5))
                    }
                }
    }

    @Test
    fun startNodeWithGtxStrictConfigurationFalse() {
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: strictmode
                    compile:
                      strictGtvConversion: false
                      rellVersion: 0.13.9
                """.trimIndent())
            }
            addSourceFile("strictmode.rell", """
                module;
                operation strict_gtv(arg: integer) {}
            """.trimIndent())
        }

        TestProcess.Builder("node", "start", "--wipe")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .start {
                    TestProcess.Builder("tx", "--cid", "0", "strict_gtv", "25L", "--await").startCondition("was posted CONFIRMED").start()
                }
    }

    @Test
    fun legacyTypeConversionDefaultNonStrictBehaviour() {
        // This test captures the behavior of strict_gtv for compile targets before rell 0.13.9
        testData(dir) {
            config {
                blockchains("""
                    blockchains:
                      hello:
                        module: strictmode
                    compile:
                      rellVersion: 0.13.8
                """.trimIndent())
            }
            addSourceFile("strictmode.rell", """
                module;
                operation strict_gtv(arg: integer) {}
            """.trimIndent())
        }

        TestProcess.Builder("node", "start", "--wipe")
                .setWorkingDir(dir.toFile())
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .start {
                    TestProcess.Builder("tx", "--cid", "0", "strict_gtv", "25L", "--await").startCondition("was posted CONFIRMED").start()
                }
    }
}
