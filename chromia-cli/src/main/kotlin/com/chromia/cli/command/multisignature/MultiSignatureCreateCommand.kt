package com.chromia.cli.command.multisignature

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.ft.addFtAuthenticationOperation
import com.chromia.cli.tools.ft.initFtAuth
import com.chromia.cli.model.ChromiaModel
import com.chromia.cli.tools.config.optionalChromiaModelConfigOption
import com.chromia.cli.util.LocalDeploymentOption
import com.chromia.cli.util.RemoteDeploymentOption
import com.chromia.cli.util.getFormattedUtcDateTime
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.core.PrintMessage
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.arguments.transformAll
import com.github.ajalt.clikt.parameters.groups.OptionGroup
import com.github.ajalt.clikt.parameters.groups.cooccurring
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import java.nio.file.Paths
import net.postchain.common.PropertiesFileLoader
import net.postchain.common.toHex
import net.postchain.crypto.PubKey
import net.postchain.gtv.GtvString
import net.postchain.gtv.parse.GtvParser

class MultiSignatureCreateCommand : ChromiaCommand(name = "create", help = "Creates a new transaction for multi signature and signs it with your key") {

    private val settings by optionalChromiaModelConfigOption()
    private val explicitTarget by LocalDeploymentOption({ settings.config })
    private val deploymentTarget by RemoteDeploymentOption { settings.model ?: ChromiaModel.default() }.cooccurring()

    private val ftAuthOptions by object : OptionGroup("FT compatible dapps options") {
        val ftAuth by option(help = "Adds ft4.ft_auth operation for FT-compatible dapps").flag()
        val ftAccountId by option(help = "Explicitly specify which account to use")
        val ftAuthDescriptorId by option("--auth-descriptor-id", "-id", help = "Explicitly specify which auth descriptor id to use")
    }
    private val secret by secretOption()

    private val fileWithSigners by option("--signers-file", help = "Path to file containing public keys of signers (pubkey1=x,pubkey2=y,...)")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()

    private val outputFolder by option("--target", help = "Path where file should be saved")
            .file()
            .default(Paths.get("").toAbsolutePath().toFile())

    private val opName by argument(help = "name of the operation to execute.")

    private val args by argument(help = "arguments to pass to the operation.", helpTags = mapOf(
            "integer" to "123",
            "big_integer" to "1234L",
            "string" to "foo, \"bar\"",
            "bytearray" to "will be encoded using the rell notation x\"<myByteArray>\" and will initially be interpreted as a hex-string.",
            "array" to "[foo,123]",
            "dict" to """["key1":value1,"key2":value2]"""
    ))
            .multiple()
            .transformAll { args ->
                args.map {
                    try {
                        GtvParser.parse(it)
                    } catch (_: IllegalArgumentException) {
                        GtvString(it)
                    }
                }
            }

    override fun run() {
        val target = deploymentTarget ?: explicitTarget
        val postchainClientConfig = settings.config.setApiUrls(target.url).setBrid(target.brid)
        secret?.let { postchainClientConfig.setSignerFromSecret(it.toPath()) }
        val client = target.createClient(postchainClientConfig)

        val signers = getSignersFromFile()
        val initialSigner = postchainClientConfig.signers
        require(initialSigner.isNotEmpty()) { "No initial signer found. Either set one in your configuration or specify path to secret file" }

        val signersWithoutInitialSigner = signers.filter { it != initialSigner.first().pubKey }.toList()

        echo("Creating transaction with signers: ${listOf(initialSigner.firstOrNull()?.pubKey) + signersWithoutInitialSigner}")
        val transactionBuilder = client.transactionBuilder(initialSigner, signersWithoutInitialSigner)

        if (ftAuthOptions.ftAuth) {
            require(ftAuthOptions.ftAuthDescriptorId != null) { "Must specify auth descriptor id when using ft auth for multi signature" }
            initFtAuth(client)

            val signerPubkey = (postchainClientConfig.signers.singleOrNull()?.pubKey
                    ?: throw PrintMessage("A single keypair is required to use FT authentication", statusCode = 1))
            addFtAuthenticationOperation(client, transactionBuilder, opName, signerPubkey.data, ftAuthOptions.ftAccountId, ftAuthOptions.ftAuthDescriptorId)
        }

        val transaction = transactionBuilder.addOperation(opName, *args.toTypedArray())
                .addNop()
                .build()
        saveTransactionToFile(transaction)
    }

    private fun getSignersFromFile(): Set<PubKey> {
        val properties = PropertiesFileLoader.load(fileWithSigners.path)
        val signers = mutableListOf<PubKey>()

        val keys = properties.keys
        while (keys.hasNext()) {
            val key = keys.next()
            val value = properties.getString(key)
            try {
                val pubkey = PubKey(value)
                signers.add(pubkey)
            } catch (e: IllegalArgumentException) {
                throw PrintMessage("Failed to add signer for value: $value, reason: ${e.message}. Please verify that your signers file is defined correctly", 1)
            }
        }
        return signers.toSet()
    }

    private fun saveTransactionToFile(transaction: ByteArray) {
        val file = outputFolder.resolve("${opName}_transaction_${getFormattedUtcDateTime()}")
        file.writeText(transaction.toHex())
        echo("Transaction is written as hex to file: ${file.absolutePath}")
    }
}
