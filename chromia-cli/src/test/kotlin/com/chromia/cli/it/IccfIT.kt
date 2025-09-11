package com.chromia.cli.it

import com.chromia.api.ChromiaCompileApi
import com.chromia.api.filterBlockchains
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.compile.withSigner
import com.chromia.build.tools.testData
import com.chromia.cli.command.node.INITILIZED_LOG
import com.chromia.cli.compile.NodeConfig
import com.chromia.cli.model.BlockchainModel
import com.chromia.cli.model.parseModel
import net.postchain.base.gtv.GtvToBlockchainRidFactory
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.impl.PostchainClientImpl
import net.postchain.client.request.SingleEndpointPool
import net.postchain.common.BlockchainRid
import net.postchain.common.toHex
import net.postchain.crypto.KeyPair
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.mapper.toObject
import net.postchain.rell.api.base.RellCliEnv
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class IccfIT {

    private val sourceChainName = "source_chain"

    @Test
    fun iccfIT(@TempDir dir: Path) {

        testData(dir) {
            config {
                blockchains(
                        """
                           blockchains:
                                $sourceChainName:
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
                operation confirmation1(tx: gtx_transaction) {
                    require(op_context.get_all_operations()[0].name == "iccf_proof");
                    require(text.from_gtv(tx.body.operations[0].args[0]) == "Secret message");
                }
                operation confirmation2(first_arg: text, tx: gtx_transaction) {
                    require(op_context.get_all_operations()[0].name == "iccf_proof");
                    require(text.from_gtv(tx.body.operations[0].args[0]) == "Secret message");
                }
            """.trimIndent())
        }

        // If test fails verify that we are getting the correct brid from getSourceChainBrid, by adding .verbose()
        // option to the outer most TestProcess.Builder(...) and look for the brid for source_chain.
        val sourceChainBrid = getSourceChainBrid(dir)
        TestProcess.Builder("node", "start", "--directory-chain-mock", "--wipe")
                .awaitCompletion(false)
                .startCondition(INITILIZED_LOG)
                .setConfig(dir.resolve("chromia.yml").toFile())
                .start {
                    TestProcess.Builder("tx", "--cid", "1", "op_to_confirm", "Secret message", "--await").verbose().startCondition("was posted and confirmed").start()
                    val config = PostchainClientConfig(BlockchainRid.ZERO_RID, endpointPool = SingleEndpointPool("http://localhost:7740"), queryByChainId = 1)
                    val client = PostchainClientImpl(config)
                    val txRid = client.query("get_latest_tx", gtv(mapOf())).asByteArray().toHex()
                    TestProcess.Builder("tx", "--cid", "2", "--iccf-source", sourceChainBrid, "--iccf-tx", txRid, "confirmation1", "--await").startCondition("was posted and confirmed").start()
                    TestProcess.Builder("tx", "--cid", "2", "--iccf-source", sourceChainBrid, "--iccf-tx", txRid, "--iccf-arg-pos", "1", "confirmation2", "some_arg", "--await").startCondition("was posted and confirmed").start()
                }
    }

    private fun getSourceChainBrid(dir: Path): String {
        val cliEnv = RellCliEnv.DEFAULT
        val chromiaYml = dir.resolve("chromia.yml")

        val sourceChainModel = parseModel(chromiaYml).filterBlockchains { bc, model ->
            model.type == BlockchainModel.Type.BLOCKCHAIN && bc == sourceChainName
        }

        // In StartCommand we are using the default key pairs from node config to add as signers to the gtv
        val defaultNodeConfigUsedInStartCommand = NodeConfig.getDefaultNodeConfig(sourceChainModel)
        val keypair = KeyPair.of(defaultNodeConfigUsedInStartCommand.pubKey, defaultNodeConfigUsedInStartCommand.privKey)

        return ChromiaCompileApi.build(cliEnv, sourceChainModel).toList().map { (_, gtv) ->
            val gtvWithSigners = withSigner(gtv, keypair.pubKey.data)
            GtvToBlockchainRidFactory.calculateBlockchainRid(gtvWithSigners.toObject()).toHex()
        }.first()
    }
}

