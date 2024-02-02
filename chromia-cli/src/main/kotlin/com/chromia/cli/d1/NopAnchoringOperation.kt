package com.chromia.cli.d1

import net.postchain.core.TxEContext
import net.postchain.gtx.GTXOperation
import net.postchain.gtx.data.ExtOpData

/**
 * Operation which does nothing
 */
class NopAnchoringOperation(extOpData: ExtOpData) : GTXOperation(extOpData) {
    override fun checkCorrectness() {
        require(data.args.size == 3) { "Wrong number of arguments to $ANCHOR_BLOCK_HEADER" }
    }

    override fun apply(ctx: TxEContext): Boolean {
        return true
    }

    companion object {
        const val ANCHOR_BLOCK_HEADER = "__anchor_block_header"
    }
}
