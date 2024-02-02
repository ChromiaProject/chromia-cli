package com.chromia.cli.d1

import net.postchain.common.BlockchainRid
import net.postchain.common.hexStringToByteArray
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvByteArray
import net.postchain.gtv.GtvString
import net.postchain.gtv.gtvml.GtvMLParser


object ManagementChainFactory {
    fun createManagementChain() = GtvMLParser.parseGtvML(this::class.java.getResource("template.xml")!!.readText())
}

fun gtvToByteArray(gtv: Gtv) = when (gtv) {
    is GtvByteArray -> gtv.asByteArray()
    is GtvString -> gtv.asString().hexStringToByteArray()
    else -> throw IllegalArgumentException("Cannot convert $gtv to ByteArray")
}

fun gtvToBlockchainRid(gtv: Gtv) = when (gtv) {
    is GtvByteArray -> BlockchainRid(gtv.asByteArray())
    is GtvString -> BlockchainRid.buildFromHex(gtv.asString())
    else -> throw IllegalArgumentException("Cannot convert $gtv to blockchain rid")
}
