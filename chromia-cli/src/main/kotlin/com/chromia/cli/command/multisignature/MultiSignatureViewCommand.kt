package com.chromia.cli.command.multisignature

import com.chromia.build.tools.multisignature.MultiSignatureTxData
import com.chromia.cli.base.formatter.json
import com.chromia.cli.command.ChromiaCommand
import com.chromia.cli.tools.multisignature.parseTransactionFile
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import net.postchain.common.toHex
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvArray
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvNull
import net.postchain.gtx.Gtx

class MultiSignatureViewCommand : ChromiaCommand(name = "view", help = "View a existing transaction") {

    private val transactionFile by option("-f", "--file", help = "Path to file of transaction")
            .file(canBeDir = false, mustExist = true, mustBeReadable = true)
            .required()

    override fun run() {
        val txData = MultiSignatureTxData.parseTransactionFile(transactionFile)
        echo(json(parseTransactionGtxForJson(txData)))
    }

    private fun parseTransactionGtxForJson(txData: MultiSignatureTxData): Map<String, Any> {
        val transaction = txData.transaction
        val transactionGtx = Gtx.decode(transaction)

        return mapOf(
                "transactionRID" to txData.txRid.toHex(),
                "blockchainRID" to transactionGtx.gtxBody.blockchainRid.toHex(),
                "operations" to
                        transactionGtx.gtxBody.operations.map { op ->
                            mapOf(
                                    "operation" to op.opName,
                                    "arguments" to op.args.map { arg -> argumentParser(arg) }

                            )
                        },
                "signers" to transactionGtx.gtxBody.signers.map { signer -> signer.toHex() },
                "signatures" to transactionGtx.signatures.map { signature -> signature.toHex() }
        )
    }

    private fun argumentParser(arg: Gtv): Any {
        return when (arg) {
            is GtvArray -> arg.array.map { argumentParser(it) }
            is GtvDictionary -> arg.asDict().mapValues { (_, v) -> argumentParser(v) }
            is GtvNull -> "null"
            else -> arg.getRawGtv().toString()
        }
    }
}