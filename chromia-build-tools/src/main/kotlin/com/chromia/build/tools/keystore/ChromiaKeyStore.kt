package com.chromia.build.tools.keystore

import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import net.postchain.crypto.KeyPair

class ChromiaKeyStore(val keyId: String = "chromia_key") {

    private val chromiaHome = System.getenv("CHROMIA_HOME") ?: (System.getProperty("user.home") + "/.chromia")
    private val publicKeyFile = Path("$chromiaHome/$keyId.pubkey")
    private val privateKeyFile = Path("$chromiaHome/$keyId")

    fun saveKeyPair(keyPair: KeyPair) {
        if (findKeyPair() != null) {
            throw IllegalArgumentException("Key pair with keyId: $keyId already exists in $chromiaHome")
        }

        Path(chromiaHome).createDirectories()
        publicKeyFile.writeText(keyPair.pubKey.hex())
        privateKeyFile.writeText(keyPair.privKey.hex())

        println(
                """
            |Keypair is written to $chromiaHome. To use this key pair, set key.id = $keyId in your configuration file
        """.trimMargin()
        )
    }

    fun loadKeyPair(): KeyPair {
        return findKeyPair()
                ?: throw IllegalArgumentException("Could not find key pair with key id: $keyId in $chromiaHome")
    }

    fun findKeyPair(): KeyPair? {
        if (publicKeyFile.exists() && privateKeyFile.exists()) {
            return KeyPair.of(publicKeyFile.readText(), privateKeyFile.readText())
        }
        return null
    }
}
