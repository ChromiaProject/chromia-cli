package com.chromia.cli.util

import net.postchain.crypto.Secp256K1CryptoSystem
import org.junit.jupiter.api.Test
import java.io.File

class ChromiaKeyManagerTest {


    @Test
    fun `test chromia key manager`() {
        val cs = Secp256K1CryptoSystem()
        val (chromiaKeypair, wordlist) = cs.generateKeyPairWithMnemonic()


        val password = "securePassword"
        val file = File("encrypted_data.dat")

        ChromiaKeyPairStorage.encryptAndPersist(chromiaKeypair, password, file)

        val decryptedKeys = ChromiaKeyPairStorage.loadAndDecrypt("2", file)

        val a = 2

    }
}