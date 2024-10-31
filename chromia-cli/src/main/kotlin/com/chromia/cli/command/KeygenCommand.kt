package com.chromia.cli.command

import com.chromia.build.tools.keystore.ChromiaKeyStore
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.groups.mutuallyExclusiveOptions
import com.github.ajalt.clikt.parameters.groups.required
import com.github.ajalt.clikt.parameters.groups.single
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.toHex
import net.postchain.crypto.KeyPair
import net.postchain.crypto.Secp256K1CryptoSystem
import java.io.File
import java.io.FileOutputStream
import java.util.*

class KeygenCommand : ChromiaCommand(name = "keygen", help = "Generates public/private key pair") {

    private val wordList by option(
            "-m", "--mnemonic",
            help = """
            Mnemonic word list, words separated by space, e.g:
                "lift employ roast rotate liar holiday sun fever output magnet...""
        """.trimIndent()
    )
            .default("")

    private val fileToGenerate: GeneratedFile? by mutuallyExclusiveOptions(name = "File format",
            option1 = option("-f", "--file", help = "Set file to save keypair to explicitly")
                    .file(canBeDir = false)
                    .convert { GeneratedFile.PropertiesFile(it.name, it.parentFile, it) },
            option2 = option("--key-id", help = "Name the generated key with an id")
                    .convert { GeneratedFile.KeyIdFile(it) }
    )
            .single()
            .required()


    private val dry by option("--dry", help = "Perform dry run, prints keys in terminal and does not save keys to disk").flag()

    /**
     * Cryptographic key generator. Will generate a pair of public and private keys and print to stdout.
     */
    override fun run() {
        val (keyPair, mnemonic) = generateSecp256k1KeyPairWithMnemonic(wordList)
        if (dry) {
            echo("""
                    |mnemonic:  $mnemonic 
                    |pubkey:    ${keyPair.pubKey.data.toHex()}
                    |privkey:   ${keyPair.privKey.data.toHex()}
                """.trimMargin())
        } else {
            when (fileToGenerate) {
                is GeneratedFile.KeyIdFile -> {
                    val name = (fileToGenerate as GeneratedFile.KeyIdFile).name
                    val chromiaKeyStore = ChromiaKeyStore(name)
                    val existingKeyPair = chromiaKeyStore.findKeyPair()
                    if (existingKeyPair != null) {
                        throw PrintMessage("Keypair with id: ${chromiaKeyStore.keyId} already exists", 1)
                    }
                    val target = chromiaKeyStore.saveKeyPair(keyPair)
                    val mnemonicFile = saveSecp256k1Mnemonic(mnemonic, File("$target/${name}_mnemonic"), keyPair)
                    echo("""
                |Mnemonic is written to ${mnemonicFile.absolutePath}, take appropriate action on the content of the file to make sure it is kept safe
                |pubkey:    ${keyPair.pubKey.data.toHex()}
            """.trimMargin()
                    )
                }

                is GeneratedFile.PropertiesFile -> {
                    val fileObject = (fileToGenerate as GeneratedFile.PropertiesFile)
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

                null -> {}
            }
        }
    }
}

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
                This is a generated file that contains you mnemonic phrase to recover your keypair with public key ${keyPair.pubKey.hex()}.
                It is highly recommended that you delete this file from your system once the phrase has been placed in a secure place or moved this file to a secure place. 
                Mnemonic phrase generated:
                $mnemonic
            """.trimIndent()
    )

    return file
}

sealed class GeneratedFile {
    data class PropertiesFile(val name: String, val directory: File?, val file: File) : GeneratedFile()
    data class KeyIdFile(val name: String) : GeneratedFile()
}