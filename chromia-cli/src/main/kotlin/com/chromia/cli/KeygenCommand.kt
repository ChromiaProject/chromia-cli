package com.chromia.cli

import com.chromia.cli.CryptoSystemType.DILITHIUM
import com.chromia.cli.CryptoSystemType.ECDSA
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.groups.default
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.toHex
import net.postchain.crypto.*
import net.postchain.crypto.pqc.dilithium.DilithiumCryptoSystem
import org.bitcoinj.crypto.MnemonicCode
import java.io.File
import java.io.FileOutputStream
import java.util.*

enum class CryptoSystemType(val option: String) {
    ECDSA("--ecdsa"),
    DILITHIUM("--dilithium")
}

class KeygenCommand : CliktCommand(name = "keygen", help = "Generates public/private key pair") {

    private val wordList by option(
            "-m", "--mnemonic",
            help = """
            Mnemonic word list, words separated by space, e.g:
                "lift employ roast rotate liar holiday sun fever output magnet...""
        """.trimIndent()
    )
            .default("")

    private val file by option("-s", "--save", help = "File to save the generated keypair in")
            .file(canBeDir = false)

    private val cs by mutuallyExclusiveOptions(
            option(ECDSA.option, help = "ECDSA keys").flag().convert { ECDSA },
            option(DILITHIUM.option, help = "Dilithium keys").flag().convert { DILITHIUM },
            name = "Provider tier",
    ).default(ECDSA)

    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    override fun run() {
        if (cs == ECDSA) {
            val (keyPair, mnemonic) = generateSecp256k1KeyPairWithMnemonic(wordList)

            file?.let {
                saveKeyPair(keyPair, it.absoluteFile)
            }
            echo(
                    """
            |privkey:   ${keyPair.privKey.data.toHex()}
            |pubkey:    ${keyPair.pubKey.data.toHex()}
            |mnemonic:  $mnemonic 
        """.trimMargin()
            )

        } else if (cs == DILITHIUM) {
            if (file == null) {
                error("--save option must be specified in case of ${DILITHIUM.option}")
            }

            val keyPair = generateDilithiumKeyPair()
            file?.let {
                saveKeyPair(keyPair, it)
            }
            echo(keyPair.pubKey.data.toHex())
        }
    }
}

private fun generateSecp256k1KeyPairWithMnemonic(wordList: String): Pair<KeyPair, String> {
    val cs = Secp256K1CryptoSystem()

    var privKey = cs.generatePrivKey().data
    val mnemonicInstance = MnemonicCode.INSTANCE
    var mnemonic = mnemonicInstance.toMnemonic(privKey).joinToString(" ")
    if (wordList.isNotEmpty()) {
        val words = wordList.split(" ")
        mnemonicInstance.check(words)
        mnemonic = wordList
        privKey = mnemonicInstance.toEntropy(words)
    }

    val pubKey = secp256k1_derivePubKey(privKey)

    val keyPair = KeyPair(PubKey(pubKey), PrivKey(privKey))
    return keyPair to mnemonic
}

private fun saveKeyPair(keyPair: KeyPair, file: File) {
    if (file.parentFile != null && !file.parentFile.exists()) file.parentFile.mkdirs()
    val properties = Properties()
    properties["privkey"] = keyPair.privKey.data.toHex()
    properties["pubkey"] = keyPair.pubKey.data.toHex()

    FileOutputStream(file).use { fs ->
        properties.store(fs, "Keypair generated")
        fs.flush()
    }
}

fun generateDilithiumKeyPair(): KeyPair = DilithiumCryptoSystem().generateKeyPair()
