package com.chromia.build.tools.icmf

import net.postchain.base.BaseBlockBuilderExtension
import net.postchain.base.TxEventSink
import net.postchain.base.data.BaseBlockBuilder
import net.postchain.common.BlockchainRid
import net.postchain.core.BlockEContext
import net.postchain.core.TxEContext
import net.postchain.gtv.Gtv

const val ICMF_MESSAGE_TYPE = "icmf_message"

class InMemoryIcmfBlockBuilderExtension: BaseBlockBuilderExtension, TxEventSink {
    private lateinit var blockchainRid: BlockchainRid

    override fun init(blockEContext: BlockEContext, baseBB: BaseBlockBuilder) {
        blockchainRid = baseBB.blockchainRID
        baseBB.installEventProcessor(ICMF_MESSAGE_TYPE, this)
    }

    override fun finalize(): Map<String, Gtv> {
       return mapOf()
    }

    override fun processEmittedEvent(ctxt: TxEContext, type: String, data: Gtv) {
        InMemoryIcmfMessageQueue.addMessage(IcmfMessageOp.fromGtv(blockchainRid, data), ctxt.height)
    }
}
