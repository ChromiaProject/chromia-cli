package com.chromia.cli.command.multisignature

import com.github.ajalt.clikt.core.PrintMessage
import net.postchain.common.data.Hash
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvDecoder
import net.postchain.gtv.GtvDictionary
import net.postchain.gtv.GtvEncoder
import net.postchain.gtv.GtvFactory.gtv
import java.io.File

class MultiSignatureTxData(val transaction: ByteArray, val txRid: Hash) {
    private fun toGtv(): Gtv {
        return gtv(mapOf(
                TX_RID_KEY to gtv(txRid),
                TX_KEY to gtv(transaction)
        ))
    }

    fun encode(): String {
        return GtvEncoder.encodeGtv(toGtv()).toHex()
    }

    companion object {
        private const val TX_RID_KEY = "tx_rid"
        private const val TX_KEY = "tx"

        private fun fromGtv(gtv: Gtv): MultiSignatureTxData = when (gtv) {
            !is GtvDictionary -> throw IllegalArgumentException("Transaction data must be a dictionary")
            else -> MultiSignatureTxData(
                    transaction = gtv[TX_KEY]?.asByteArray() ?: throw IllegalArgumentException("$TX_KEY is required"),
                    txRid = gtv[TX_RID_KEY]?.asByteArray() ?: throw IllegalArgumentException("$TX_RID_KEY is required")
            )
        }

        fun decode(transactionData: String): MultiSignatureTxData {
            val gtv = GtvDecoder.decodeGtv(transactionData.hexStringToByteArray())
            return fromGtv(gtv)
        }
    }
}

fun parseTransactionFile(transactionFile: File): MultiSignatureTxData {
    return try {
        MultiSignatureTxData.decode(transactionFile.readText())
    } catch (_: IllegalArgumentException) {
        throw PrintMessage("Transaction file is incompatible with current CLI version", 1)
    }
}