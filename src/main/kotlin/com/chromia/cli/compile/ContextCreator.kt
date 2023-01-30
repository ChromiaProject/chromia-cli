package com.chromia.cli.compile

import com.chromia.cli.parser.ModuleArgs.getModuleArgsValues
import net.postchain.common.BlockchainRid
import net.postchain.gtv.Gtv
import net.postchain.gtv.GtvFactory.gtv
import net.postchain.gtv.GtvNull
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.lib.test.UnitTestBlockRunner
import net.postchain.rell.model.R_App
import net.postchain.rell.module.RellPostchainModuleEnvironment
import net.postchain.rell.runtime.*

object ContextCreator {
    data class Context(
            var globalCtx: Rt_GlobalContext,
            var chainCtx: Rt_ChainContext,
            val sqlCtx: Rt_SqlContext
    )

    fun createTestContext(
            app: R_App,
            coProperties: C_CompilerOptions,
            blockchainRid: BlockchainRid,
            sqlMapper: Rt_ChainSqlMapping,
            moduleArgs: Map<String, Map<String, Gtv>>
    ): Context {
        val globalCtx = createGlobalContext(coProperties)
        val chainCtx = Rt_ChainContext(GtvNull, getModuleArgsValues(app, moduleArgs), blockchainRid)
        val sqlCtx = Rt_RegularSqlContext.createNoExternalChains(app, sqlMapper)
        return Context(globalCtx, chainCtx, sqlCtx)
    }

    fun createGlobalContext(compilerOptions: C_CompilerOptions): Rt_GlobalContext {
        return Rt_GlobalContext(compilerOptions, Rt_OutPrinter, Rt_OutPrinter, RellPostchainModuleEnvironment.DEFAULT)
    }
}
