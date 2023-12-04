package com.chromia.build.tools.icmf

import net.postchain.core.EContext
import net.postchain.gtx.SimpleGTXModule


class InMemoryIcmfSenderGtxModule : SimpleGTXModule<InMemoryIcmfSenderGtxModule.Context>(Context(), mapOf(), mapOf()) {
    override fun initializeDB(ctx: EContext) {}
    override fun makeBlockBuilderExtensions() =  listOf(InMemoryIcmfBlockBuilderExtension())

    class Context { }
}
