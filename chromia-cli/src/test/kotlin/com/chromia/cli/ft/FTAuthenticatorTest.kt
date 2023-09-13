package com.chromia.cli.ft

import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.mordant.terminal.Terminal
import net.postchain.client.exception.ClientError
import net.postchain.common.hexStringToByteArray
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock

class FTAuthenticatorTest {

    @Test
    fun incompatibleDappTest() {
        val authenticator = FTAuthenticator(
                { query, _ -> if (query == "ft4.get_version") throw ClientError("", null, "Query not found", null) else GtvNull },
                Terminal())
        assertThrows<CliktError> {
            authenticator.addAuthenticationOperation(mock(), "my_op", PubKey("11".repeat(32).hexStringToByteArray()), null)
        }
    }
}
