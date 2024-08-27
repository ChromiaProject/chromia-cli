package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import java.io.File
import java.nio.file.Path
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir


class VotersetIT {

    companion object {
        val testBrid = BlockchainRid("0000000000000000000000000000000000000000000000000000000000000001".hexStringToByteArray())
    }

    @Test
    fun `Add dapp provider test`(@TempDir dir: Path) {
        val container = "testcontainer"
        testData(dir) {
            secret()
            config {
                deployments(
                        """
                    deployments:
                      test:
                        url: "$apiUrl"
                        brid: x"${BlockchainRid.ZERO_RID.toHex()}"
                        container: $container
                        chains:
                          hello: x"${testBrid.toHex()}"
                """.trimIndent()
                )
            }
        }
        val secretFile = File(dir.toFile(), ".chromia/config").toPath().toString()
        val pubKey = TestDataBuilder.keyPair.pubKey
        val newPubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val economyChainBrid = BlockchainRid.buildFromHex("0000000000000000000000000000000000000000000000000000000000000002")

        val directoryChainTxRecorder = TxRecorderModel(BlockchainRid.ZERO_RID, mapOf(
                "get_economy_chain_rid" to gtv(economyChainBrid),
                "cm_get_blockchain_api_urls" to gtv(listOf(gtv(apiUrl)))
        ))

        val economyChainTxRecorder = TxRecorderModel(
                economyChainBrid,
                mapOf(
                        "ft4.get_accounts_by_signer" to gtv(mapOf("data" to gtv(gtv(mapOf("id" to gtv("3".repeat(64).hexStringToByteArray())))))),
                        "ft4.get_account_auth_descriptors_by_signer" to gtv(mapOf("data" to gtv(gtv(mapOf(
                                "id" to gtv("4".repeat(64).hexStringToByteArray()),
                                "args" to gtv(gtv(gtv("A")), gtv(pubKey.data)),
                                "created" to gtv(System.currentTimeMillis()),
                                "auth_type" to gtv("A"),
                                "rules" to GtvNull
                        ))))),
                        "ft4.get_auth_flags" to gtv(gtv("A")),
                        "get_economy_chain_rid" to gtv(economyChainBrid),
                        "ft4.get_version" to gtv("0.2.0")
                )
        )

        withModel(
                economyChainTxRecorder,
                directoryChainTxRecorder

        ) {
            TestProcess.Builder("deployment", "voterset", "add-dapp-provider", "--network", "test", "--container-id", container, "--pubkey", "$newPubKey", "--secret", secretFile, "--no-await")
                    .verbose()
                    .setWorkingDir(dir.toFile())
                    .start()

            val gtx = Gtx.decode(economyChainTxRecorder.txList.single())
            val operations = gtx.gtxBody.operations
            assertThat(operations[0].opName).isEqualTo("ft4.ft_auth")
            assertThat(operations[0].args).containsExactly(gtv("3".repeat(64).hexStringToByteArray()), gtv("4".repeat(64).hexStringToByteArray()))
            assertThat(operations[1].opName).isEqualTo("register_dapp_provider")
            assertThat(operations[1].args).containsExactly(gtv(container), gtv(newPubKey.data))
        }
    }
}
