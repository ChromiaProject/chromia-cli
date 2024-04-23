package com.chromia.build.tools

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.chromia.build.tools.config.ChromiaConfig
import com.chromia.build.tools.keystore.ChromiaKeyStore
import java.io.File
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.common.BlockchainRid
import net.postchain.crypto.KeyPair
import org.apache.commons.configuration2.PropertiesConfiguration
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ChromiaConfigTest {


    @Test
    fun `secret file takes precedence over key id`(@TempDir dir: Path) {
        val keyPair = KeyPair.of("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765", "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")
        val keyIdName = "keyIdUsedForTesting"

        val pubKeySecretFile = "039B9ED551D5BDCC52FF9418ED77FBA7D761B24B7D06596829771A6DEA50E613AD"
        val privKeySecretFile = "D33345577D6E08997D35D3D359DAF6CD4AF91651B2C006F9974E4B73E06574F7"
        val secretFile = File(dir.toFile(), ".secret")
        with(secretFile) {
            writeText("""
                pubkey=$pubKeySecretFile
                privkey=$privKeySecretFile
            """.trimIndent())
        }

        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            val config = PropertiesConfiguration()
            config.setProperty("keyId", keyIdName)
            val clientConfig = ChromiaConfig(config).get(
                    blockchainRid = BlockchainRid.ZERO_RID,
                    apiurl = "url",
                    secret = secretFile)
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(pubKeySecretFile)
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(privKeySecretFile)
        }
    }

    @Test
    fun `secret file takes precedence over key id oaaa`(@TempDir dir: Path) {
        val pubKey = "02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765"
        val privKey = "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429"
        val keyPair = KeyPair.of(pubKey, privKey)
        val keyIdName = "keyIdUsedForTesting"

        EnvironmentVariables("CHROMIA_HOME", dir.absolutePathString()).execute {
            ChromiaKeyStore(keyIdName).saveKeyPair(keyPair)
            val config = PropertiesConfiguration()
            config.setProperty("keyId", keyIdName)
            val clientConfig = ChromiaConfig(config).get(
                    blockchainRid = BlockchainRid.ZERO_RID,
                    apiurl = "url",
                    secret = null)
            assertThat(clientConfig.signers.size).isEqualTo(1)
            assertThat(clientConfig.signers.first().pubKey.hex()).isEqualTo(pubKey)
            assertThat(clientConfig.signers.first().privKey.hex()).isEqualTo(privKey)
        }
    }
}
