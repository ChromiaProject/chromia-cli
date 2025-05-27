package com.chromia.cli.command.multisignature

import com.github.ajalt.clikt.core.PrintMessage
import mu.KLogging
import net.postchain.common.data.Hash
import net.postchain.common.hexStringToByteArray
import net.postchain.common.toHex
import net.postchain.gtv.GtvDecoder.decodeGtv
import net.postchain.gtv.GtvEncoder.encodeGtv
import net.postchain.gtv.mapper.GtvObjectMapper.fromGtv
import net.postchain.gtv.mapper.GtvObjectMapper.toGtvDictionary
import java.io.File

class MultiSignatureTxData(val transaction: ByteArray, val txRid: Hash) {
    fun encode() = encodeGtv(toGtvDictionary(this)).toHex()

    companion object : KLogging() {
        fun parseTransactionFile(transactionFile: File): MultiSignatureTxData {
            return try {
                decode(transactionFile.readText().trim())
            } catch (e: IllegalArgumentException) {
                logger.error(e) { "Error while parsing transaction file" }
                throw PrintMessage("Transaction file is incompatible with current CLI version", 1)
            }
        }

        fun decode(transactionData: String): MultiSignatureTxData {
            val gtv = decodeGtv(transactionData.hexStringToByteArray())
            return fromGtv(gtv, MultiSignatureTxData::class)
        }
    }
}
