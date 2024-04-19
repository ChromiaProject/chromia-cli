package com.chromia.cli.command

import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import net.postchain.common.toHex
import net.postchain.crypto.KeyPair
import net.postchain.crypto.PrivKey
import net.postchain.crypto.PubKey
import net.postchain.crypto.Secp256K1CryptoSystem
import net.postchain.crypto.secp256k1_derivePubKey
import org.bitcoinj.crypto.MnemonicCode

class KeygenCommand : CliktCommand(name = "keygen", help = "Generates public/private key pair") {

    private val wordList by option(
            "-m", "--mnemonic",
            help = """
            Mnemonic word list, words separated by space, e.g:
                "lift employ roast rotate liar holiday sun fever output magnet...""
        """.trimIndent()
    )
            .default("")

    private val file by option("-s", "--save", help = "Set file to save keypair to explicitly")
            .file(canBeDir = false)

    private val deprecated by option("-dr", "--deprecated-recovery", help = "Used to recover keys from Mnemonic generated before version 0.13.2, will be removed in the future")
            .flag()

    private val keyId by option("--key-id", help = "Name the generated key with an id")

    private val dry by option("--dry", help = "Perform dry run, does not save keys to disk").flag()

    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    override fun run() {
        val (keyPair, mnemonic) = generateSecp256k1KeyPairWithMnemonic(wordList, deprecated)

        println(
                """
            |privkey:   ${keyPair.privKey.data.toHex()}
            |pubkey:    ${keyPair.pubKey.data.toHex()}
            |mnemonic:  $mnemonic 
        """.trimMargin()
        )

        when {
            dry -> return
            file != null -> saveSecp256k1KeyPair(keyPair, file!!.absoluteFile)
            else -> {
                val chromiaKeyStore = keyId?.let { ChromiaKeyStore(it) } ?: ChromiaKeyStore()
                val existingKeyPair = chromiaKeyStore.findKeyPair()
                if (existingKeyPair != null) {
                    throw PrintMessage("Keypair with id: ${chromiaKeyStore.keyId} already exists", 1)
                }
                chromiaKeyStore.saveKeyPair(keyPair)
            }
        }
    }
}

private fun generateSecp256k1KeyPairWithMnemonic(wordList: String, deprecated: Boolean = false): Pair<KeyPair, String> {
    val cs = Secp256K1CryptoSystem()

    if (deprecated) {
        check(wordList.isNotEmpty()) { "Mnemonic is needed to use --deprecated-recovery" }
    }

    // Recover the mnemonic from old Secp256K1CryptoSystem were the mnemonic was not bip39 compatible
    if (wordList.isNotEmpty() && deprecated) {
        val words = wordList.split(" ")
        val mnemonicInstance = MnemonicCode.INSTANCE
        mnemonicInstance.check(words)
        val privKey = mnemonicInstance.toEntropy(words)
        val pubKey = secp256k1_derivePubKey(privKey)
        val keyPair = KeyPair(PubKey(pubKey), PrivKey(privKey))
        return keyPair to wordList

    } else if (wordList.isNotEmpty() && !deprecated) {
        // New Secp256K1CryptoSystem were the mnemonic is bip39 compatible
        return cs.recoverKeyPairFromMnemonic(wordList)
    }

    return cs.generateKeyPairWithMnemonic()
}

private fun saveSecp256k1KeyPair(keyPair: KeyPair, file: File) {
    if (file.parentFile != null && !file.parentFile.exists()) file.parentFile.mkdirs()
    val properties = Properties()
    properties["privkey"] = keyPair.privKey.data.toHex()
    properties["pubkey"] = keyPair.pubKey.data.toHex()

    FileOutputStream(file).use { fs ->
        properties.store(fs, "Keypair generated using secp256k1")
        fs.flush()
    }
}
