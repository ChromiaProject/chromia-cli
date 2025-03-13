package com.chromia.cli.command.deployment.voterset

import com.chromia.build.tools.restapi.DirectoryChainModel
import com.chromia.build.tools.restapi.RestApiInstance
import com.chromia.build.tools.restapi.RestApiInstance.withModel
import com.chromia.build.tools.restapi.TestModel
import com.chromia.build.tools.restapi.withValidConfiguration
import com.chromia.build.tools.testData
import com.chromia.cli.tools.ft.addEvmAuthOperation
import com.chromia.cli.tools.ft.addFtAuthOperation
import com.chromia.cli.util.DeploymentTestDataCreator
import com.chromia.directory1.cm_api.CM_GET_BLOCKCHAIN_API_URLS
import com.chromia.directory1.economy_chain.API_VERSION
import com.chromia.directory1.economy_chain.GET_FT4_ACCOUNT_IDS
import com.chromia.directory1.economy_chain.REGISTER_DAPP_PROVIDER
import com.chromia.directory1.economy_chain_in_directory_chain.GET_ECONOMY_CHAIN_RID
import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_AUTH_DESCRIPTOR_COUNTER
import com.chromia.directory1.lib.ft4.external.auth.EVM_AUTH
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_MESSAGE_TEMPLATE
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.GET_VERSION
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.testing.test
import com.github.ajalt.mordant.terminal.TerminalRecorder
import io.mockk.*
import net.postchain.api.rest.controller.Model
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.types.WrappedByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.mapper.GtvObjectMapper
import net.postchain.gtx.GtxQuery
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertContains

internal class VotersetAddDappProviderCommandTest {
    companion object {
        private val DAPP_PROVIDER_ID = "1".repeat(64).hexStringToWrappedByteArray()
        private val TEST_AUTH_DESCRIPTOR_ID = "2".repeat(64).hexStringToWrappedByteArray()
        private val TEST_PUB_KEY = "3".repeat(64).hexStringToWrappedByteArray()
        private val TEST_EVM_KEY = "4".repeat(40).hexStringToWrappedByteArray()
        private var ACCOUNT_ID = "5".repeat(64).hexStringToWrappedByteArray()
        private var CONTAINER_ID = "6".repeat(64)
        private const val SIGNER_PUB_KEY = "03ECD350EEBC617CBBFBEF0A1B7AE553A748021FD65C7C50C5ABB4CA16D4EA5B05"
        private const val SIGNER_PRIV_KEY = "BBBDFE956021912512E14BB081B27A35A0EABC4098CB687E973C434006BCE114"

    }

    @TempDir
    private lateinit var testDir: Path
    private lateinit var settingsFile: File
    private val logger = TerminalRecorder(width = 1000, outputInteractive = false)
    val model = DirectoryChainModel().withValidConfiguration()

    @BeforeEach
    fun setup() {
        DeploymentTestDataCreator.unitTestApp(testDir)
        settingsFile = testDir.resolve("chromia.yml").toFile()
    }

    @AfterEach
    fun tearDown() {
        logger.clearOutput()
    }

    @Test
    fun testEvmAuthenticationToken(@TempDir dir: Path) {
        testData(dir) {
            secret {
                secretFile(dir)
            }
        }
        val secretFile = File(dir.toFile(), ".secret")

        val (evmAuthCalled, ftAuthCalled) = setupAuthMocks()

        withModel(VotersetAddModel(model.blockchainRid, TEST_EVM_KEY)) {
            val response = VotersetAddDappProviderCommand().test(listOf(
                    "--network", "test",
                    "--settings", settingsFile.absolutePath,
                    "--secret", secretFile.absolutePath,
                    "--evm-auth", TEST_EVM_KEY.toHex(),
                    "--container-id", CONTAINER_ID,
                    "--pubkey", TEST_PUB_KEY.toHex(),
            ))

            assertContains(response.stdout, "CONFIRMED")
            verify(exactly = 1) { evmAuthCalled() }
            verify(exactly = 0) { ftAuthCalled() }
        }
        unmockkAll()
    }


    @Test
    fun testAuthenticationWithoutEvmToken(@TempDir dir: Path) {
        testData(dir) {
            secret {
                secretFile(dir)
            }
        }
        val secretFile = File(dir.toFile(), ".secret")
        secretFile.writeText(
                """
                privkey=$SIGNER_PRIV_KEY
                pubkey=$SIGNER_PUB_KEY
                """.trimIndent()
        )

        val (evmAuthCalled, ftAuthCalled) = setupAuthMocks()

        withModel(VotersetAddModel(model.blockchainRid, SIGNER_PUB_KEY.hexStringToWrappedByteArray())) {
            val response = VotersetAddDappProviderCommand().test(listOf(
                    "--network", "test",
                    "--settings", settingsFile.absolutePath,
                    "--secret", secretFile.absolutePath,
                    "--container-id", CONTAINER_ID,
                    "--pubkey", TEST_PUB_KEY.toHex(),
            ))

            assertContains(response.stdout, "CONFIRMED")
            verify(exactly = 0) { evmAuthCalled() }
            verify(exactly = 1) { ftAuthCalled() }
        }
        unmockkAll()
    }

    private fun setupAuthMocks(): Pair<() -> Unit, () -> Unit> {
        val evmAuthCalled = mockk<() -> Unit>(relaxed = true)
        val ftAuthCalled = mockk<() -> Unit>(relaxed = true)

        mockkStatic("com.chromia.cli.tools.ft.EvmAuthKt")
        every {
            any<CoreCliktCommand>().addEvmAuthOperation(any(), any(), any(), any(), any(), any(), any(), any(), any())
        } answers {
            evmAuthCalled()
        }

        mockkStatic("com.chromia.cli.tools.ft.FtAuthKt")
        every {
            any<CoreCliktCommand>().addFtAuthOperation(any(), any(), any())
        } answers {
            ftAuthCalled()
        }
        return evmAuthCalled to ftAuthCalled
    }

    internal class VotersetAddModel(val model: Model, signerKey: WrappedByteArray) : Model by model {
        constructor(blockchainRid: BlockchainRid, signerKey: WrappedByteArray)
                : this(TestModel(blockchainRid), signerKey)

        private val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = TEST_AUTH_DESCRIPTOR_ID,
                args = gtv(gtv(gtv("A"), gtv("T")), gtv(signerKey)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = ACCOUNT_ID
        )

        override fun query(query: GtxQuery): Gtv {
            return when (query.name) {
                API_VERSION -> gtv(33)
                GET_VERSION -> gtv("0.4.0")
                CM_GET_BLOCKCHAIN_API_URLS -> gtv(listOf(gtv(RestApiInstance.apiUrl)))
                GET_ACCOUNTS_BY_SIGNER ->
                    GtvObjectMapper.toGtvDictionary(PagedResult(
                            nextCursor = null,
                            data = listOf(gtv((mapOf("id" to gtv(descriptor.accountId)))))
                    ))

                GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER -> gtv(GtvObjectMapper.toGtvDictionary(descriptor))
                GET_AUTH_FLAGS -> gtv(listOf(gtv("A"), gtv("T")))
                GET_FT4_ACCOUNT_IDS -> gtv(listOf(gtv(ACCOUNT_ID)))
                EVM_AUTH -> gtv(mapOf("signature" to gtv(DAPP_PROVIDER_ID)))
                REGISTER_DAPP_PROVIDER -> gtv(mapOf("id" to gtv(TEST_PUB_KEY)))
                GET_ECONOMY_CHAIN_RID -> gtv(model.blockchainRid)
                GET_AUTH_MESSAGE_TEMPLATE -> gtv("{blockchain_rid} auth message template {nonce}")
                GET_AUTH_DESCRIPTOR_COUNTER -> gtv(17)
                else -> throw IllegalStateException("${query.name}  is not supported")
            }
        }
    }
}

