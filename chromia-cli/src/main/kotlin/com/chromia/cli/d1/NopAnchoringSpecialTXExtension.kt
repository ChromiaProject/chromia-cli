package com.chromia.cli.d1

import com.chromia.cli.d1.NopAnchoringOperation.Companion.ANCHOR_BLOCK_HEADER
import net.postchain.base.SpecialTransactionPosition
import net.postchain.base.data.PostgreSQLDatabaseAccess
import net.postchain.common.BlockchainRid
import net.postchain.core.BlockEContext
import net.postchain.crypto.CryptoSystem
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtx.GTXModule
import net.postchain.gtx.data.OpData
import net.postchain.gtx.special.GTXSpecialTxExtension

/**
 * Adds a special transaction which anchors previous block data into the current. (self-anchor)
 * To be used with [NopAnchoringGTXModule] to be able to respond to the anchoring api
 *
 * NOT TO BE USED IN PRODUCTION
 */
class NopAnchoringSpecialTXExtension : GTXSpecialTxExtension {
    override fun createSpecialOperations(position: SpecialTransactionPosition, bctx: BlockEContext): List<OpData> {
        if (bctx.height == 0L) return listOf()
        val rid = db.getBlockRID(bctx, bctx.height - 1) ?: ByteArray(0)
        val block = db.getBlock(bctx, rid)!!
        return listOf(OpData(ANCHOR_BLOCK_HEADER, arrayOf(gtv(rid), gtv(block.blockHeader), gtv(block.witness))))
    }

    override fun getRelevantOps() = setOf(ANCHOR_BLOCK_HEADER)

    override fun init(module: GTXModule, chainID: Long, blockchainRID: BlockchainRid, cs: CryptoSystem) {}

    override fun needsSpecialTransaction(position: SpecialTransactionPosition): Boolean = when (position) {
        SpecialTransactionPosition.Begin -> true
        SpecialTransactionPosition.End -> false
    }

    override fun validateSpecialOperations(position: SpecialTransactionPosition, bctx: BlockEContext, ops: List<OpData>): Boolean {
        return true
    }

    companion object {
        val db = PostgreSQLDatabaseAccess()
    }
}
