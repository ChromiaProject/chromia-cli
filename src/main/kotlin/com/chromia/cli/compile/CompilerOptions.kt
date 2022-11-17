package com.chromia.cli.compile

import net.postchain.rell.compiler.base.core.C_AtAttrShadowing
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourcePath
import net.postchain.rell.model.R_LangVersion

class CompilerOptions(
        private val rellVersion: String,
        private val gtv: Boolean,
        private val deprecatedError: Boolean,
        private val ide: Boolean,
        private val blockCheck: Boolean,
        private val testLib: Boolean,
        private val hiddenLib: Boolean,
        private val allowDbModificationsInObjectExprs: Boolean,
        private val symbolInfoFile: List<String>?
) {
    fun getCompilerOptions (): C_CompilerOptions {
        return C_CompilerOptions(
                R_LangVersion.of(rellVersion),
                gtv = gtv,
                deprecatedError = deprecatedError,
                ide = ide,
                blockCheck = blockCheck,
                C_AtAttrShadowing.DEFAULT,
                testLib = testLib,
                hiddenLib = hiddenLib,
                allowDbModificationsInObjectExprs = allowDbModificationsInObjectExprs,
                symbolInfoFile = if(symbolInfoFile == null) null else ( C_SourcePath.of(symbolInfoFile))
        )
    }
}