package com.chromia.cli.it

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import com.chromia.build.tools.TestDataBuilder
import com.chromia.build.tools.TestProcess
import com.chromia.build.tools.restapi.RestApiInstance.apiUrl
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.testData
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.economy_chain_in_directory_chain.GET_ECONOMY_CHAIN_RID
import com.chromia.lib.ft4.core.accounts.AuthType
import com.chromia.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.lib.ft4.utils.PagedResult
import com.chromia.lib.ft4.version.GET_VERSION
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.Gtx
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path


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
                GET_ECONOMY_CHAIN_RID to gtv(economyChainBrid),
                CM_GET_BLOCKCHAIN_API_URLS to gtv(listOf(gtv(apiUrl)))
        ))

        val economyChainTxRecorder = TxRecorderModel(
                economyChainBrid,
                mapOf(
                        GET_ACCOUNTS_BY_SIGNER to GtvObjectMapper.toGtvDictionary(PagedResult(
                                nextCursor = null,
                                data = listOf(gtv((mapOf("id" to gtv("3".repeat(64).hexStringToByteArray())))))

                        )),
                        GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER to gtv(GtvObjectMapper.toGtvDictionary(Ft4GetAccountAuthDescriptorsBySignerResult(
                                id = "4".repeat(64).hexStringToWrappedByteArray(),
                                args = gtv(gtv(gtv("A")), gtv(pubKey.data)),
                                created = System.currentTimeMillis(),
                                authType = AuthType.S,
                                rules = GtvNull,
                                accountId = "5".repeat(64).hexStringToWrappedByteArray()
                        ))),
                        GET_AUTH_FLAGS to gtv(gtv("A")),
                        GET_ECONOMY_CHAIN_RID to gtv(economyChainBrid),
                        GET_VERSION to gtv("0.4.0")
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
