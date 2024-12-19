package com.chromia.cli.ft

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.core.auth.Signature
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_AUTH_DESCRIPTOR_COUNTER
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_MESSAGE_TEMPLATE
import com.chromia.directory1.lib.ft4.external.auth.evmAuthOperation
import com.chromia.directory1.lib.ft4.external.auth.ftAuthOperation
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.GET_VERSION
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.parse
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.config.PostchainClientConfig
import net.postchain.client.core.PostchainClient
import net.postchain.client.exception.ClientError
import net.postchain.client.transaction.TransactionBuilder
import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.common.wrap
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.mapper.GtvObjectMapper
import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.util.concurrent.TimeUnit


class FTAuthenticatorTest {

    val testCommand = NoOpCliktCommand().context { terminal = Terminal(interactive = false) }.also { it.parse(arrayOf()) }

    @Test
    fun incompatibleDappTest() {
        assertThrows<CliktError> {
            testCommand.initFtAuth { query, _ -> if (query == GET_VERSION) throw ClientError("", null, "Query not found", null) else GtvNull }
        }
    }

    @Test
    fun validV1AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val res = assertThrows<PrintMessage> {
            testCommand.initFtAuth { query, _ -> queryResponseV1(pubKey, listOf("A"), query) }
        }
        assertThat(res.message).isEqualTo("Versions before release 0.4.0 are not supported, current FT4 version 0.1.1 is to old")
    }

    @Test
    fun validV2AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val res = assertThrows<PrintMessage> {
            testCommand.initFtAuth { query, _ -> queryResponseV2(pubKey, listOf("A"), query) }
        }
        assertThat(res.message).isEqualTo("Versions before release 0.4.0 are not supported, current FT4 version 0.2.0 is to old")
    }

    @Test
    fun validV4AuthDescriptorTypeS() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val accountId = "5".repeat(64).hexStringToByteArray()
        val authDescriptorId = "6".repeat(64).hexStringToByteArray()
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = authDescriptorId.wrap(),
                args = gtv(gtv(gtv("A")), gtv(pubKey.data)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = accountId.wrap()
        )
        val client: (String, Gtv) -> Gtv = { query, _ -> queryResponseV4(listOf("A"), query, descriptor) }
        testCommand.initFtAuth(client)

        val transactionBuilder = mock<TransactionBuilder>()
        assertDoesNotThrow {
            testCommand.addFtAuthenticationOperation(client, transactionBuilder, "my_op", pubKey.data, null)
        }
        verify(transactionBuilder).ftAuthOperation(accountId, authDescriptorId)
    }

    @Test
    fun validV4AuthDescriptorTypeSMultiDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val accountId = "5".repeat(64).hexStringToByteArray()
        val authDescriptorId = "6".repeat(64).hexStringToByteArray()
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = authDescriptorId.wrap(),
                args = gtv(gtv(gtv("A")), gtv(pubKey.data)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = accountId.wrap()
        )
        val descriptor2 = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = "7".repeat(64).hexStringToWrappedByteArray(),
                args = gtv(gtv(gtv("A")), gtv(pubKey.data)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = accountId.wrap()
        )
        val client: (String, Gtv) -> Gtv = { query, _ -> queryResponseV4MultipleDescriptors(listOf("A"), query, listOf(descriptor, descriptor2)) }
        testCommand.initFtAuth(client)

        val transactionBuilder = mock<TransactionBuilder>()
        assertDoesNotThrow {
            testCommand.addFtAuthenticationOperation(client, transactionBuilder, "my_op", pubKey.data, null)
        }
        verify(transactionBuilder).ftAuthOperation(accountId, authDescriptorId)
    }

    @Test
    fun validV4AuthDescriptorTypeM() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val accountId = "5".repeat(64).hexStringToByteArray()
        val authDescriptorId = "6".repeat(64).hexStringToByteArray()
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = authDescriptorId.wrap(),
                args = gtv(gtv(gtv("A")), gtv(1), gtv(gtv(pubKey.data))),
                created = System.currentTimeMillis(),
                authType = AuthType.M,
                rules = GtvNull,
                accountId = accountId.wrap()
        )

        val client: (String, Gtv) -> Gtv = { query, _ -> queryResponseV4(listOf("A"), query, descriptor) }
        testCommand.initFtAuth(client)

        val transactionBuilder = mock<TransactionBuilder>()
        assertDoesNotThrow {
            testCommand.addFtAuthenticationOperation(client, transactionBuilder, "my_op", pubKey.data, null)
        }
        verify(transactionBuilder).ftAuthOperation(accountId, authDescriptorId)
    }

    @Test @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun evmAuthSuccess() {
        val evmAddress = "183B9875AC828CE1A56D5152874B2833AC8ABB63".hexStringToByteArray()
        val accountId = "5".repeat(64).hexStringToByteArray()
        val authDescriptorId = "6".repeat(64).hexStringToByteArray()
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = authDescriptorId.wrap(),
                args = gtv(gtv(gtv("A")), gtv(evmAddress)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = accountId.wrap()
        )
        val client = mock<PostchainClient> {
            on { config } doReturn PostchainClientConfig(
                    blockchainRid = BlockchainRid("3250B9CCD1FAA489BFCC9706DE55237F102DE7A5ED66D5FE6E8EE6A20823D2BD".hexStringToByteArray()),
                    endpointPool = mock(),
            )
            on { query(eq(GET_VERSION), any()) } doReturn gtv("0.4.0")
            on { query(eq(GET_ACCOUNTS_BY_SIGNER), any()) } doReturn GtvObjectMapper.toGtvDictionary(PagedResult(
                    nextCursor = null,
                    data = listOf(gtv((mapOf("id" to gtv(descriptor.accountId)))))
            ))
            on { query(eq(GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER), any()) } doReturn gtv(GtvObjectMapper.toGtvDictionary(descriptor))
            on { query(eq(GET_AUTH_FLAGS), any()) } doReturn gtv(listOf(gtv("A")))
            on { query(eq(GET_AUTH_MESSAGE_TEMPLATE), any()) } doReturn gtv("{blockchain_rid} auth message template {nonce}")
            on { query(eq(GET_AUTH_DESCRIPTOR_COUNTER), any()) } doReturn gtv(17)
        }
        testCommand.initFtAuth(client)

        val (accountId2, authDescriptorId2) = testCommand.findFtAccountIdAndAuthDescriptorId(
                client,
                null,
                evmAddress,
                "my_op",
                null)
        assertThat(accountId2).isEqualTo(accountId)
        assertThat(authDescriptorId2).isEqualTo(authDescriptorId)

        val signature = Signature(r = "1234".hexStringToWrappedByteArray(), s = "5678".hexStringToWrappedByteArray(), v = 5)

        val transactionBuilder = mock<TransactionBuilder>()
        assertDoesNotThrow {
            testCommand.addEvmAuthOperation(client, transactionBuilder, "my_op", listOf(gtv("foo")), evmAddress,
                    accountId, authDescriptorId, launchWebBrowser = false) { url ->
                val response = ApacheClient()(Request(Method.POST, "$url/signature")
                        .header("Content-Type", "application/json")
                        .body("""{"r":"0x1234","s":"0x5678","v":5}"""))
                assertThat(response.status.successful).isTrue()
            }
        }
        
        verify(transactionBuilder).evmAuthOperation(accountId, authDescriptorId, listOf(signature))
    }

    @Test @Timeout(value = 30, unit = TimeUnit.SECONDS)
    fun evmAuthFailure() {
        val evmAddress = "183B9875AC828CE1A56D5152874B2833AC8ABB63".hexStringToByteArray()
        val accountId = "5".repeat(64).hexStringToByteArray()
        val authDescriptorId = "6".repeat(64).hexStringToByteArray()
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = authDescriptorId.wrap(),
                args = gtv(gtv(gtv("A")), gtv(evmAddress)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = accountId.wrap()
        )
        val client = mock<PostchainClient> {
            on { config } doReturn PostchainClientConfig(
                    blockchainRid = BlockchainRid("3250B9CCD1FAA489BFCC9706DE55237F102DE7A5ED66D5FE6E8EE6A20823D2BD".hexStringToByteArray()),
                    endpointPool = mock(),
            )
            on { query(eq(GET_VERSION), any()) } doReturn gtv("0.4.0")
            on { query(eq(GET_ACCOUNTS_BY_SIGNER), any()) } doReturn GtvObjectMapper.toGtvDictionary(PagedResult(
                    nextCursor = null,
                    data = listOf(gtv((mapOf("id" to gtv(descriptor.accountId)))))
            ))
            on { query(eq(GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER), any()) } doReturn gtv(GtvObjectMapper.toGtvDictionary(descriptor))
            on { query(eq(GET_AUTH_FLAGS), any()) } doReturn gtv(listOf(gtv("A")))
            on { query(eq(GET_AUTH_MESSAGE_TEMPLATE), any()) } doReturn gtv("{blockchain_rid} auth message template {nonce}")
            on { query(eq(GET_AUTH_DESCRIPTOR_COUNTER), any()) } doReturn gtv(17)
        }
        testCommand.initFtAuth(client)

        val (accountId2, authDescriptorId2) = testCommand.findFtAccountIdAndAuthDescriptorId(
                client,
                null,
                evmAddress,
                "my_op",
                null)
        assertThat(accountId2).isEqualTo(accountId)
        assertThat(authDescriptorId2).isEqualTo(authDescriptorId)

        val transactionBuilder = mock<TransactionBuilder>()
        assertFailure {
            testCommand.addEvmAuthOperation(client, transactionBuilder, "my_op", listOf(gtv("foo")), evmAddress,
                    accountId, authDescriptorId, launchWebBrowser = false) { url ->
                val response = ApacheClient()(Request(Method.POST, "$url/error")
                        .body("the error"))
                assertThat(response.status.successful).isTrue()
            }
        }.isInstanceOf(CliktError::class).hasMessage("the error")

        verifyNoInteractions(transactionBuilder)
    }

    private fun queryResponseV1(pubKey: PubKey, flags: List<String>, query: String): Gtv {
        return when (query) {
            "ft4.get_version" -> gtv("0.1.1")
            "ft4.get_accounts_by_participant_id" -> gtv(listOf(gtv("2".repeat(64).hexStringToByteArray())))
            "ft4.get_account_auth_descriptors_by_participant_id" -> gtv(gtv(mapOf(
                    "id" to gtv("4".repeat(64).hexStringToByteArray()),
                    "args" to gtv(gtv(gtv("A")), gtv(pubKey.data)),
                    "created" to gtv(System.currentTimeMillis()),
                    "auth_type" to gtv("S"),
                    "rules" to GtvNull,
                    "account_id" to gtv("5".repeat(64).hexStringToByteArray())
            )))

            "ft4.get_auth_flags" -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
    }

    private fun queryResponseV2(pubKey: PubKey, flags: List<String>, query: String): Gtv {
        return when (query) {
            "ft4.get_version" -> gtv("0.2.0")
            "ft4.get_accounts_by_signer" -> gtv(mapOf("data" to gtv(gtv(mapOf("id" to gtv("3".repeat(64).hexStringToByteArray()))))))
            "ft4.get_account_auth_descriptors_by_signer" -> gtv(mapOf("data" to gtv(gtv(mapOf(
                    "id" to gtv("5".repeat(64).hexStringToByteArray()),
                    "args" to gtv(gtv(gtv("A")), gtv(pubKey.data)),
                    "created" to gtv(System.currentTimeMillis()),
                    "auth_type" to gtv("S"),
                    "rules" to GtvNull,
                    "account_id" to gtv("5".repeat(64).hexStringToByteArray())
            )))))

            "ft4.get_auth_flags" -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
    }

    private fun queryResponseV4(flags: List<String>, query: String, authDescriptor: Ft4GetAccountAuthDescriptorsBySignerResult): Gtv {
        return when (query) {
            GET_VERSION -> gtv("0.4.0")
            GET_ACCOUNTS_BY_SIGNER -> GtvObjectMapper.toGtvDictionary(PagedResult(
                    nextCursor = null,
                    data = listOf(gtv((mapOf("id" to gtv(authDescriptor.accountId)))))
            ))

            GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER -> gtv(GtvObjectMapper.toGtvDictionary(authDescriptor))
            GET_AUTH_FLAGS -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
    }

    private fun queryResponseV4MultipleDescriptors(flags: List<String>, query: String, authDescriptors: List<Ft4GetAccountAuthDescriptorsBySignerResult>): Gtv {
        return when (query) {
            GET_VERSION -> gtv("0.4.0")
            GET_ACCOUNTS_BY_SIGNER -> GtvObjectMapper.toGtvDictionary(PagedResult(
                    nextCursor = null,
                    data = listOf(gtv((mapOf("id" to gtv(authDescriptors.first().accountId)))))
            ))

            GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER -> gtv(authDescriptors.map { GtvObjectMapper.toGtvDictionary((it)) })
            GET_AUTH_FLAGS -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
    }
}
