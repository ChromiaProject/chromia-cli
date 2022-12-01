package com.chromia.cli.compile

import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvNull
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.R_App
import net.postchain.rell.model.R_ModuleName
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.rell.runtime.*
import net.postchain.rell.utils.BytesKeyPair
import net.postchain.rell.utils.RellCliUtils
import net.postchain.rell.utils.immMapOf

object ContextCreator {
    data class Context(
            var globalCtx: Rt_GlobalContext,
            var chainCtx: Rt_ChainContext,
            val sqlCtx: Rt_SqlContext,
            val keyPair: BytesKeyPair
    )
    fun createTestContext(
        app: R_App,
        coProperties: C_CompilerOptions,
        blockchainRid: BlockchainRid,
        sqlMapper: Rt_ChainSqlMapping,
        moduleArgs: Map<R_ModuleName, Rt_Value>
    ): Context {
        val globalCtx = createGlobalContext(coProperties)
        val chainCtx = Rt_ChainContext(GtvNull, moduleArgs, blockchainRid)
        val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, sqlMapper)
        val keyPair = UnitTestBlockRunner.getTestKeyPair() //this is static in the test scope Bob and alice
        return Context(globalCtx, chainCtx, sqlCtx, keyPair)
    }

    fun createGlobalContext(compilerOptions: C_CompilerOptions): Rt_GlobalContext {
        return Rt_GlobalContext(compilerOptions, Rt_OutPrinter, Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
    }

    fun createRegularAppContext(globalCtx: Rt_GlobalContext, app: R_App): Rt_AppContext {
        return Rt_AppContext(
                globalCtx,
                RellCliUtils.createChainContext(),
                app,
                repl = false,
                test = false,
                replOut = null,
                blockRunnerStrategy = Rt_UnsupportedBlockRunnerStrategy
        )
    }
}