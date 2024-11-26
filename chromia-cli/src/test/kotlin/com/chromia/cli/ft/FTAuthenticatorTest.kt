package com.chromia.cli.ft

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.directory1.lib.ft4.core.accounts.AuthType
import com.chromia.directory1.lib.ft4.external.accounts.Ft4GetAccountAuthDescriptorsBySignerResult
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNTS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.accounts.GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER
import com.chromia.directory1.lib.ft4.external.auth.GET_AUTH_FLAGS
import com.chromia.directory1.lib.ft4.utils.PagedResult
import com.chromia.directory1.lib.ft4.version.GET_VERSION
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.exception.ClientError
import net.postchain.common.hexStringToByteArray
import net.postchain.common.hexStringToWrappedByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.gtv.mapper.GtvObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

class FTAuthenticatorTest {

    @Test
    fun incompatibleDappTest() {
        assertThrows<CliktError> {
            createFTAuthenticator(
                    { query, _ -> if (query == GET_VERSION) throw ClientError("", null, "Query not found", null) else GtvNull },
                    Terminal())
        }
    }

    @Test
    fun validV1AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val res = assertThrows<PrintMessage> {
            createFTAuthenticator(
                    { query, _ -> queryResponseV1(pubKey, listOf("A"), query) },
                    Terminal()

            )
        }
        assertThat(res.message).isEqualTo("Versions before release 0.4.0 are not supported, current FT4 version 0.1.1 is to old")
    }

    @Test
    fun validV2AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val res = assertThrows<PrintMessage> {
            createFTAuthenticator(
                    { query, _ -> queryResponseV2(pubKey, listOf("A"), query) },
                    Terminal()

            )
        }
        assertThat(res.message).isEqualTo("Versions before release 0.4.0 are not supported, current FT4 version 0.2.0 is to old")
    }

    @Test
    fun validV4AuthDescriptorTypeS() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = "5".repeat(64).hexStringToWrappedByteArray(),
                args = gtv(gtv(gtv("A")), gtv(pubKey.data)),
                created = System.currentTimeMillis(),
                authType = AuthType.S,
                rules = GtvNull,
                accountId = "5".repeat(64).hexStringToWrappedByteArray()
        )
        val authenticator = createFTAuthenticator(
                { query, _ -> queryResponseV4(listOf("A"), query, descriptor) },
                Terminal()

        )
        assertDoesNotThrow {
            authenticator.addAuthenticationOperation(mock(), "my_op", pubKey, null)
        }
    }

    @Test
    fun validV4AuthDescriptorTypeM() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val descriptor = Ft4GetAccountAuthDescriptorsBySignerResult(
                id = "5".repeat(64).hexStringToWrappedByteArray(),
                args = gtv(gtv(gtv("A")), gtv(1), gtv(gtv(pubKey.data))),
                created = System.currentTimeMillis(),
                authType = AuthType.M,
                rules = GtvNull,
                accountId = "5".repeat(64).hexStringToWrappedByteArray()
        )

        val authenticator = createFTAuthenticator(
                { query, _ -> queryResponseV4(listOf("A"), query, descriptor) },
                Terminal()

        )
        assertDoesNotThrow {
            authenticator.addAuthenticationOperation(mock(), "my_op", pubKey, null)
        }
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
                    data = listOf(gtv((mapOf("id" to gtv("3".repeat(64).hexStringToByteArray())))))
            ))

            GET_ACCOUNT_AUTH_DESCRIPTORS_BY_SIGNER -> gtv(GtvObjectMapper.toGtvDictionary(authDescriptor))
            GET_AUTH_FLAGS -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
    }
}
