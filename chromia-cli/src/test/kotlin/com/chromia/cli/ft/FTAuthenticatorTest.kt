package com.chromia.cli.ft

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.exception.ClientError
import net.postchain.common.hexStringToByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

class FTAuthenticatorTest {

    @Test
    fun incompatibleDappTest() {
        assertThrows<CliktError> {
            FTAuth.createFTAuthenticator(
                { query, _ -> if (query == "ft4.get_version") throw ClientError("", null, "Query not found", null) else GtvNull },
                Terminal())
        }
    }

    @Test
    fun validV1AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val authenticator = FTAuth.createFTAuthenticator(
                { query, _ -> queryResponseV1(pubKey, listOf("A"), query) },
                Terminal()

        )
        assertDoesNotThrow {
            authenticator.addAuthenticationOperation(mock(), "my_op", pubKey, null)
        }
    }

    @Test
    fun validV2AuthDescriptor() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())
        val authenticator = FTAuth.createFTAuthenticator(
                { query, _ -> queryResponseV2(pubKey, listOf("A"), query) },
                Terminal()

        )
        assertDoesNotThrow {
            authenticator.addAuthenticationOperation(mock(), "my_op", pubKey, null)
        }
    }

    @Test
    fun authDescriptorMissingOneFlag() {
        val pubKey = PubKey("1".repeat(64).hexStringToByteArray())

        val authenticator = FTAuth.createFTAuthenticator(
                { query, _ -> queryResponseV1(pubKey, listOf("A", "B"), query) },
                Terminal()

        )
        val throwable = assertThrows<PrintMessage> {
            authenticator.addAuthenticationOperation(mock(), "my_op", pubKey, null)
        }
        assertThat(throwable.message).isEqualTo("No valid account descriptor found. Operation my_op requires the flag(s): [A, B], while the flag(s) of the auth descriptor is: [A]")
    }

    private fun queryResponseV1(pubKey: PubKey, flags: List<String>, query: String): Gtv {
        return when (query) {
            "ft4.get_version" -> gtv("0.1.1")
            "ft4.get_accounts_by_participant_id" -> gtv(listOf(gtv("2".repeat(64).hexStringToByteArray())))
            "ft4.get_account_auth_descriptors_by_participant_id" -> gtv(gtv(mapOf(
                    "id" to gtv("4".repeat(64).hexStringToByteArray()),
                    "args" to gtv(gtv(gtv("A")), gtv(pubKey.data)),
                    "created" to gtv(System.currentTimeMillis()),
                    "auth_type" to gtv("A"),
                    "rules" to GtvNull
            )))

            "ft4.get_auth_flags" -> gtv(flags.map { gtv(it) })
            else -> GtvNull
        }
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
                "auth_type" to gtv("A"),
                "rules" to GtvNull
        )))))

        "ft4.get_auth_flags" -> gtv(flags.map { gtv(it) })
        else -> GtvNull
    }

}
