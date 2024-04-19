package com.chromia.build.tools.keystore

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import net.postchain.crypto.KeyPair
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables

class ChromiaKeyStoreTest {

    private val keyPair = KeyPair.of("02CCF1F5FF6A6E5C9A6E89716A67BC77BECEF4DA804BD3BCE3105D96EB3D1AD765", "7EEBCE9FF2339D21CA3F4A325C9968B0E6D197A2CADA421F7DB8DEFD02AB1429")

    @TempDir
    private lateinit var testDir: Path
    private lateinit var chromiaKeyStore: ChromiaKeyStore

    @BeforeEach
    fun setupKeyStory() {
        EnvironmentVariables("CHROMIA_HOME", testDir.absolutePathString()).execute {
            chromiaKeyStore = ChromiaKeyStore()
        }
    }

    @Test
    fun `Save and load key pair flow`() {
        chromiaKeyStore.saveKeyPair(keyPair)
        val loadedKeyPair = chromiaKeyStore.loadKeyPair()
        assertThat(loadedKeyPair.pubKey.hex()).isEqualTo(keyPair.pubKey.hex())
        assertThat(loadedKeyPair.privKey.hex()).isEqualTo(keyPair.privKey.hex())
    }

    @Test
    fun `Save and find key pair flow`() {
        assertThat(chromiaKeyStore.findKeyPair()).isNull()
        chromiaKeyStore.saveKeyPair(keyPair)
        val foundKeyPair = chromiaKeyStore.findKeyPair()
        assertThat(foundKeyPair).isNotNull()
        assertThat(foundKeyPair!!.pubKey.hex()).isEqualTo(keyPair.pubKey.hex())
        assertThat(foundKeyPair.privKey.hex()).isEqualTo(keyPair.privKey.hex())
    }

    @Test
    fun `loadKeyPair throws when load key pair do not find expected keys`() {
        val throwable = assertThrows<IllegalArgumentException> { chromiaKeyStore.loadKeyPair() }
        assertThat(throwable.message).isEqualTo("Could not find key pair with key id: chromia_key in $testDir")
    }

    @Test
    fun `saveKeyPair throws when key pair with same id already exists`() {
        chromiaKeyStore.saveKeyPair(keyPair)
        val throwable = assertThrows<IllegalArgumentException> { chromiaKeyStore.saveKeyPair(keyPair) }
        assertThat(throwable.message).isEqualTo("Key pair with keyId: chromia_key already exists in $testDir")
    }
}
