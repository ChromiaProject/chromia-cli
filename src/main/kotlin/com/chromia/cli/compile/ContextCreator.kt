package com.chromia.cli.compile

import net.postchain.common.BlockchainRid
import net.postchain.gtv.GtvNull
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.R_App
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.rell.runtime.*
import net.postchain.rell.utils.BytesKeyPair
import net.postchain.rell.utils.immMapOf

class ContextCreator {
    data class Context(
            var globalCtx: Rt_GlobalContext,
            var chainCtx: Rt_ChainContext,
            val sqlCtx: Rt_SqlContext,
            val keyPair: BytesKeyPair
    )
    fun createTestContext(app: R_App,  coProperties: C_CompilerOptions, blockchainRid: BlockchainRid, sqlMapper: Rt_ChainSqlMapping): Context {
        val globalCtx = createGlobalContext(coProperties)
        val chainCtx = Rt_ChainContext(GtvNull, immMapOf(), blockchainRid)
        val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, sqlMapper)
        val keyPair = UnitTestBlockRunner.getTestKeyPair() //this is static in the test scope Bob and alice
        return Context(globalCtx, chainCtx, sqlCtx, keyPair)
    }

    fun createGlobalContext(compilerOptions: C_CompilerOptions): Rt_GlobalContext {
        return Rt_GlobalContext(compilerOptions, Rt_OutPrinter, Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
    }
}