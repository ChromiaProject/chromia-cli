package com.chromia.build.tools

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.config.ChromiaClientConfig
import com.chromia.build.tools.keystore.ChromiaKeyStore
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.crypto.KeyPair
import org.apache.commons.configuration2.PropertiesConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ChromiaConfigTest {

    @Test
    fun `Keystore key gets populated in config`(@TempDir dir: Path) {
        val pubKey = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
        val privKey = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"
        val keyPair = KeyPair.of(pubKey, privKey)
        val keyIdName = "keyIdUsedForTesting"

        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            val config = PropertiesConfiguration()
            config.setProperty("key.id", keyIdName)
            val clientConfig = ChromiaClientConfig.from(config)
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(pubKey)
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(privKey)
        }
    }
}
