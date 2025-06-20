package com.chromia.cli.command

import com.chromia.build.tools.config.ChromiaConfigLoader
import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.optionalValue
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.toHex
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import java.io.File
import java.io.FileOutputStream
import java.util.Properties
import kotlin.io.path.absolutePathString

class KeygenCommand : ChromiaCommand(name = "keygen", help = "Generates public/private key pair") {

    val configLoader = ChromiaConfigLoader { msg -> echo(msg, err = true) }

    private val wordList by option(
            "-m", "--mnemonic",
            help = """
            Mnemonic word list, words separated by space, e.g:
                "lift employ roast rotate liar holiday sun fever output magnet...""
        """.trimIndent()
    )
            .default("")

    private val keygenOutputMode: KeygenOutputMode? by mutuallyExclusiveOptions(
            name = "File format",
            option1 = option("-f", "--file", help = "Set file to save keypair to explicitly")
                    .file(canBeDir = false)
                    .convert { KeygenOutputMode.PropertiesFile(it.name, it.parentFile, it) },

            option2 = option("--get-pubkey",
                    help = "Print the active public key. If no key-id is specified, the key is determined from your configuration; otherwise, retrieves the public key for the given key-id.",
                    metavar = "<key-id>"
            ).convert { KeygenOutputMode.GetPubkey(it) }
                .optionalValue(KeygenOutputMode.GetPubkey("")),

            options = arrayOf(
                    option("--dry", help = "Perform dry run, prints keys in terminal and does not save keys to disk").flag()
                            .convert { KeygenOutputMode.DryRun },
                    option("--key-id", help = "Name the generated key with an id")
                            .convert { KeygenOutputMode.KeyIdFile(it) }
            )

    ).single().required()


    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    override fun run() {

        val (keyPair, mnemonic) = generateSecp256k1KeyPairWithMnemonic(wordList)

        when (keygenOutputMode) {
            is KeygenOutputMode.KeyIdFile -> {
                val name = (keygenOutputMode as KeygenOutputMode.KeyIdFile).name
                val chromiaKeyStore = ChromiaKeyStore(name)
                val existingKeyPair = chromiaKeyStore.findKeyPair()
                if (existingKeyPair != null) {
                    throw PrintMessage("Keypair with id: ${chromiaKeyStore.keyId} already exists", 1)
                }
                chromiaKeyStore.saveKeyPair(keyPair, mnemonic)
                echo("""
                |Mnemonic is written to ${chromiaKeyStore.mnemonicFile.absolutePathString()}, take appropriate action on the content of the file to make sure it is kept safe
                |pubkey:    ${keyPair.pubKey.data.toHex()}
            """.trimMargin()
                )
            }

            is KeygenOutputMode.PropertiesFile -> {
                val fileObject = (keygenOutputMode as KeygenOutputMode.PropertiesFile)
                saveSecp256k1KeyPair(keyPair, fileObject.file)
                val mnemonicFilePath = if (fileObject.directory == null) {
                    "${fileObject.name}_mnemonic"
                } else {
                    "${fileObject.directory}/${fileObject.name}_mnemonic"
                }
                val mnemonicFile = saveSecp256k1Mnemonic(mnemonic, File(mnemonicFilePath), keyPair)
                echo("""
                    |Keypair is written to ${fileObject.file.absolutePath}
                    |Mnemonic is written to ${mnemonicFile.absolutePath}, take appropriate action on the content of the file to make sure it is kept safe
                    |pubkey:    ${keyPair.pubKey.data.toHex()}
                """.trimMargin()
                )
            }

            is KeygenOutputMode.DryRun -> {
                echo("""
                    |mnemonic:  $mnemonic 
                    |pubkey:    ${keyPair.pubKey.data.toHex()}
                    |privkey:   ${keyPair.privKey.data.toHex()}
                """.trimMargin())
            }

            is KeygenOutputMode.GetPubkey -> {
                val keyId = (keygenOutputMode as KeygenOutputMode.GetPubkey).keyId

                val pubkey = keyId.takeIf { it.isNotBlank() }?.let {
                    retrievePublicKeyFrom(it)
                } ?: retrieveActivePubKey(configLoader)

                echo("pubkey: $pubkey")
            }

            else -> {
                PrintMessage("No keypair was generated, must provide one of --file, --key-id, --dry")
            }
        }
    }

}

private fun retrievePublicKeyFrom(keyId: String) =
        ChromiaKeyStore(keyId).findKeyPair()?.pubKey?.data?.toHex()
                ?: throw PrintMessage("No keypair found for key-id: $keyId", statusCode = 1)

private fun retrieveActivePubKey(configLoader: ChromiaConfigLoader) =
        configLoader.loadProperties().getString("pubkey")
                ?: throw PrintMessage(
                        "No pubkey found, in either your local/global configuration or the active key-id",
                        statusCode = 1
                )

private fun generateSecp256k1KeyPairWithMnemonic(wordList: String): Pair<KeyPair, String> {
    val cs = Secp256K1CryptoSystem()

    if (wordList.isNotEmpty()) {
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

private fun saveSecp256k1Mnemonic(mnemonic: String, file: File, keyPair: KeyPair): File {
    if (file.parentFile != null && !file.parentFile.exists()) file.parentFile.mkdirs()
    file.writeText(
            """
                This is a generated file that contains your mnemonic phrase to recover your keypair with public key ${keyPair.pubKey.hex()}.
                It is highly recommended that you delete this file from your system once the phrase has been placed in a secure place or moved this file to a secure place. 
                Mnemonic phrase generated:
                $mnemonic
        """.trimIndent()
    )

    return file
}

sealed class KeygenOutputMode {
    data class PropertiesFile(val name: String, val directory: File?, val file: File) : KeygenOutputMode()
    data class KeyIdFile(val name: String) : KeygenOutputMode()
    data object DryRun : KeygenOutputMode()
    data class GetPubkey(val keyId: String) : KeygenOutputMode()
}