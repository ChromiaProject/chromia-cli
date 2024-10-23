package com.chromia.cli.command.multisignature

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaConfigOption
import com.chromia.cli.util.getFormattedUtcDateTime
import com.chromia.cli.util.secretOption
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import java.io.File
import net.postchain.client.transaction.signTransaction
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex

class MultiSignatureSignCommand : ChromiaCommand(name = "sign", help = "Sign a existing transaction with your key") {

    private val transactionFile by option("-f", "--file", help = "Path to file of transaction")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()

    private val chromiaConfig by chromiaConfigOption()

    private val secret by secretOption()

    private val outputFolder by option("--target", help = "Path where file should be saved")
            .file()

    override fun run() {
        secret?.let { chromiaConfig.config.setSignerFromSecret(it.toPath()) }
        val signer = chromiaConfig.config.signers
        val transaction = transactionFile.readText().hexStringToByteArray()
        val signedTransaction = signTransaction(transaction, signer)
        saveTransactionToFile(signedTransaction)
    }

    private fun saveTransactionToFile(transaction: ByteArray) {
        val transactionName = transactionFile.name.substringBeforeLast("_")
        val targetFolder = outputFolder ?: transactionFile.parentFile.path
        val file = File("$targetFolder/${transactionName}_${getFormattedUtcDateTime()}")
        file.writeText(transaction.toHex())
        echo("Transaction is written as hex to file: ${file.absolutePath}")
    }
}
