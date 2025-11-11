package com.chromia.cli.command.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.config.chromiaConfigOption
import com.chromia.cli.tools.config.configureSigners
import com.chromia.cli.tools.config.keyPairSourceOption
import com.chromia.cli.tools.multisignature.parseTransactionFile
import com.chromia.cli.tools.util.getFormattedUtcDateTime
import com.github.ajalt.clikt.parameters.groups.provideDelegate
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.data.Hash
import net.postchain.gtx.signTransaction
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
        val txData = MultiSignatureTxData.parseTransactionFile(transactionFile)
        val signedTransaction = signTransaction(txData.transaction, txData.txRid, signer)
        saveTransactionToFile(signedTransaction, txData.txRid)
    }

    private fun saveTransactionToFile(transaction: ByteArray, txRid: Hash) {
        val txData = MultiSignatureTxData(transaction, txRid)
        val transactionName = transactionFile.name.substringBeforeLast("_")
        val targetFolder = outputFolder ?: transactionFile.parentFile.path
        val fileName = outputFileName ?: "${transactionName}_signed_${getFormattedUtcDateTime()}"
        val file = File("$targetFolder/$fileName")
        file.writeText(txData.encode())
        echo("Transaction is written as hex to file: ${file.absolutePath}")
    }
}
