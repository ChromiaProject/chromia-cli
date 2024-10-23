package com.chromia.cli.command.multisignature

import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.formatter.json
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtx.Gtx

class MultiSignatureViewCommand : ChromiaCommand(name = "view", help = "View a existing transaction") {

    private val transactionFile by option("-f", "--file", help = "Path to file of transaction")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()

    override fun run() {
        val transaction = transactionFile.readText().hexStringToByteArray()
        val transactionGtx = Gtx.decode(transaction)

        echo(json(parseTransactionGtxForJson(transactionGtx)))
    }

    private fun parseTransactionGtxForJson(transactionGtx: Gtx): Map<String, Any> {
        return mapOf(
                "blockchainRID" to transactionGtx.gtxBody.blockchainRid.toHex(),
                "operations" to
                        transactionGtx.gtxBody.operations.map { op ->
                            mapOf(
                                    "operation" to op.opName,
                                    "arguments" to op.args.map { arg -> arg.getRawGtv().toString() }

                            )
                        },
                "signers" to transactionGtx.gtxBody.signers.map { signer -> signer.toHex() },
                "signatures" to transactionGtx.signatures.map { signature -> signature.toHex() }
        )
    }


}