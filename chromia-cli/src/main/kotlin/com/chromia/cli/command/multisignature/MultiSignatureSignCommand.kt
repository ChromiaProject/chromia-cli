package com.chromia.cli.command.multisignature

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.util.getFormattedUtcDateTime
import com.chromia.cli.tools.config.keyPairSourceOption
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.client.transaction.signTransaction
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.crypto.sha256Digest
import net.postchain.gtv.merkle.GtvMerkleHashCalculatorV1
import java.io.File

class MultiSignatureSignCommand : ChromiaCommand(name = "sign", help = "Sign a existing transaction with your key") {

    private val transactionFile by option("-f", "--file", help = "Path to file of transaction")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .convert { it.absoluteFile }
            .required()

    private val chromiaConfig by chromiaConfigOption()

    private val keyPairSource by keyPairSourceOption()

    private val outputFolder by option("--target", help = "Path where file should be saved")
            .file()

    private val outputFileName by option("--file-name", help = "Override default name of output file")

    override fun run() {
        chromiaConfig.config.configureSigners(keyPairSource)
        val signer = chromiaConfig.config.signers
        val transaction = transactionFile.readText().hexStringToByteArray()
        // TODO use-new-algo choose merkle hash version
        val signedTransaction = signTransaction(transaction, signer, GtvMerkleHashCalculatorV1(::sha256Digest))
        saveTransactionToFile(signedTransaction)
    }

    private fun saveTransactionToFile(transaction: ByteArray) {
        val transactionName = transactionFile.name.substringBeforeLast("_")
        val targetFolder = outputFolder ?: transactionFile.parentFile.path
        val fileName = outputFileName ?: "${transactionName}_signed_${getFormattedUtcDateTime()}"
        val file = File("$targetFolder/$fileName")
        file.writeText(transaction.toHex())
        echo("Transaction is written as hex to file: ${file.absolutePath}")
    }
}
