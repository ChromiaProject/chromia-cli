package com.chromia.cli.model

import net.postchain.rell.compiler.base.core.C_AtAttrShadowing
import net.postchain.rell.compiler.base.core.C_CompilerOptions
import net.postchain.rell.compiler.base.utils.C_SourcePath
import net.postchain.rell.model.R_LangVersion

data class CompileModel(
        val rellVersion: String = "0.11.0",
        val source: String = "src",
        val target: String = "build",
        private val gtv: Boolean = false,
        private val deprecatedError: Boolean = false,
        private val ide: Boolean = false,
        private val blockCheck: Boolean = false,
        private val testLib: Boolean = false,
        private val hiddenLib: Boolean = false,
        private val allowDbModificationsInObjectExprs: Boolean = false,
        private val symbolInfoFile: List<String>? = null,
        val quiet: Boolean = true
) {
    val langVersion get() = R_LangVersion.of(rellVersion)
    fun getCompilerOptions (): C_CompilerOptions {
        return C_CompilerOptions(
                langVersion,
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
