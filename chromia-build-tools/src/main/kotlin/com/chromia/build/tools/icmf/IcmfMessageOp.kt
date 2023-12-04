package com.chromia.build.tools.icmf

import mu.KLogging
import net.postchain.common.BlockchainRid
import net.postchain.common.exception.UserMistake
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory
import net.postchain.gtx.data.OpData

// Original implementation and format found in
// https://gitlab.com/chromaway/postchain-chromia/-/blob/3.14.5/chromia-infrastructure/src/main/kotlin/net/postchain/d1/icmf/IcmfReceiverSpecialTxExtension.kt#L635
data class IcmfMessageOp(
        val sender: BlockchainRid,
        val topic: String,
        val body: Gtv
) {
    companion object : KLogging() {
        // operation __icmf_message(sender: byte_array, topic: text, body: gtv)
        const val OP_NAME = "__icmf_message"

        fun fromGtv(sender: BlockchainRid, gtv: Gtv) = IcmfMessageOp(sender, gtv["topic"]!!.asString(), gtv["body"]!!)

        fun fromOpData(opData: OpData): IcmfMessageOp? = fromOpNameAndArgs(opData.opName, opData.args)

        fun fromOpNameAndArgs(opName: String, args: Array<out Gtv>): IcmfMessageOp? {
            if (opName != OP_NAME) return null
            if (args.size != 3) {
                logger.warn("Got $OP_NAME operation with wrong number of arguments: ${args.size}")
                return null
            }

            return try {
                IcmfMessageOp(BlockchainRid(args[0].asByteArray()), args[1].asString(), args[2])
            } catch (e: UserMistake) {
                logger.warn("Got $OP_NAME operation with invalid argument types: ${e.message}")
                null
            }
        }
    }

    fun toOpData() = OpData(OP_NAME, arrayOf(GtvFactory.gtv(sender), GtvFactory.gtv(topic), body))
}
